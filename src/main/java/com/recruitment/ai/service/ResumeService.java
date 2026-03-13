package com.recruitment.ai.service;

import com.recruitment.ai.entity.Job;
import com.recruitment.ai.entity.Resume;
import com.recruitment.ai.repository.JobRepository;
import com.recruitment.ai.repository.MatchingScoreRepository;
import com.recruitment.ai.repository.ResumeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

@Service
public class ResumeService {

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private MatchingScoreRepository matchingScoreRepository;

    private final String uploadDir = "uploads/resumes/";

    @Transactional
    public Resume saveResume(MultipartFile file, Long jobId, boolean isResumeBank) throws IOException {
        // ... (rest of the method)
        Job job = jobRepository.findById(jobId).orElse(null);
        Path path = Paths.get(uploadDir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = path.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        String rawText = extractText(file);

        Resume resume = Resume.builder()
                .filename(file.getOriginalFilename())
                .filePath(filePath.toString())
                .rawText(rawText)
                .job(job)
                .isResumeBank(isResumeBank)
                .build();

        return resumeRepository.save(resume);
    }

    @Transactional
    public List<Resume> saveBulkResumes(MultipartFile[] files, Long jobId, boolean isResumeBank) throws IOException {
        java.util.List<Resume> resumes = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            resumes.add(saveResume(file, jobId, isResumeBank));
        }
        return resumes;
    }

    public List<Resume> getAllResumes() {
        return resumeRepository.findAll();
    }

    public List<Resume> getResumesByType(boolean isResumeBank) {
        return resumeRepository.findByIsResumeBank(isResumeBank);
    }

    public List<Resume> getResumesByJobId(Long jobId) {
        return resumeRepository.findByJob_Id(jobId);
    }

    public List<Resume> getResumesByJobIdAndType(Long jobId, boolean isResumeBank) {
        return resumeRepository.findByJob_IdAndIsResumeBank(jobId, isResumeBank);
    }

    public Resume getResumeById(Long id) {
        return resumeRepository.findById(id).orElse(null);
    }

    @Transactional
    public void deleteResume(Long id) {
        Resume resume = resumeRepository.findById(id).orElse(null);
        if (resume != null) {
            // 1. Delete matching scores linked to this resume
            matchingScoreRepository.deleteByResume_Id(id);

            // 2. Delete file from disk
            try {
                Path filePath = Paths.get(resume.getFilePath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                System.err.println("Error deleting file: " + e.getMessage());
            }

            // 3. Delete resume record
            resumeRepository.deleteById(id);
        }
    }

    @Transactional
    public void deleteAllResumes() {
        List<Resume> allResumes = resumeRepository.findAll();
        for (Resume resume : allResumes) {
            deleteResume(resume.getId());
        }
    }

    private String extractText(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null)
            return "";

        try (InputStream is = file.getInputStream()) {
            if (filename.toLowerCase().endsWith(".pdf")) {
                try (PDDocument document = PDDocument.load(is)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    return stripper.getText(document);
                }
            } else if (filename.toLowerCase().endsWith(".docx")) {
                try (XWPFDocument doc = new XWPFDocument(is);
                        XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                    return extractor.getText();
                }
            } else {
                return new String(file.getBytes());
            }
        } catch (Exception e) {
            System.err.println("Error extracting text from " + filename + ": " + e.getMessage());
            return "Error extracting text: " + e.getMessage();
        }
    }
}
