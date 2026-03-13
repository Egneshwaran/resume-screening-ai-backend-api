package com.recruitment.ai.controller;

import com.recruitment.ai.entity.Resume;
import com.recruitment.ai.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    @GetMapping
    public List<Resume> getAllResumes() {
        return resumeService.getAllResumes();
    }

    @GetMapping("/job/{jobId}")
    public List<Resume> getResumesByJob(
            @PathVariable Long jobId,
            @RequestParam(value = "isResumeBank", defaultValue = "false") boolean isResumeBank) {
        return resumeService.getResumesByJobIdAndType(jobId, isResumeBank);
    }

    @PostMapping("/upload")
    public ResponseEntity<Resume> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam("jobId") Long jobId,
            @RequestParam(value = "isResumeBank", defaultValue = "false") boolean isResumeBank) {
        try {
            return ResponseEntity.ok(resumeService.saveResume(file, jobId, isResumeBank));
        } catch (java.io.IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/bulk-upload")
    public ResponseEntity<List<Resume>> uploadBulkResumes(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("jobId") Long jobId,
            @RequestParam(value = "isResumeBank", defaultValue = "false") boolean isResumeBank) {
        try {
            return ResponseEntity.ok(resumeService.saveBulkResumes(files, jobId, isResumeBank));
        } catch (java.io.IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resume> getResumeById(@PathVariable Long id) {
        Resume resume = resumeService.getResumeById(id);
        if (resume != null) {
            return ResponseEntity.ok(resume);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(@PathVariable Long id) {
        resumeService.deleteResume(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllResumes() {
        resumeService.deleteAllResumes();
        return ResponseEntity.ok().build();
    }
}
