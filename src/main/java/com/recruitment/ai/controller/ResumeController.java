package com.recruitment.ai.controller;

import com.recruitment.ai.entity.Resume;
import com.recruitment.ai.entity.User;
import com.recruitment.ai.repository.UserRepository;
import com.recruitment.ai.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return userRepository.findByUsername(username).orElse(null);
    }

    @GetMapping
    public List<Resume> getAllResumes() {
        User user = getCurrentUser();
        return resumeService.getAllResumesByUser(user);
    }

    @GetMapping("/job/{jobId}")
    public List<Resume> getResumesByJob(@PathVariable Long jobId, @RequestParam(defaultValue = "false") boolean isResumeBank) {
        return resumeService.getResumesByJobIdAndType(jobId, isResumeBank);
    }

    @PostMapping("/upload")
    public ResponseEntity<Resume> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam("jobId") Long jobId,
            @RequestParam(value = "isResumeBank", defaultValue = "false") boolean isResumeBank) throws IOException {
        User user = getCurrentUser();
        return ResponseEntity.ok(resumeService.saveResume(file, jobId, isResumeBank, user));
    }

    @PostMapping("/bulk-upload")
    public ResponseEntity<List<Resume>> uploadBulkResumes(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("jobId") Long jobId,
            @RequestParam(value = "isResumeBank", defaultValue = "false") boolean isResumeBank) throws IOException {
        User user = getCurrentUser();
        return ResponseEntity.ok(resumeService.saveBulkResumes(files, jobId, isResumeBank, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resume> getResumeById(@PathVariable Long id) {
        Resume resume = resumeService.getResumeById(id);
        User user = getCurrentUser();
        if (resume != null && resume.getUser() != null && resume.getUser().getId().equals(user.getId())) {
            return ResponseEntity.ok(resume);
        }
        return ResponseEntity.status(403).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(@PathVariable Long id) {
        Resume resume = resumeService.getResumeById(id);
        User user = getCurrentUser();
        if (resume != null && resume.getUser() != null && resume.getUser().getId().equals(user.getId())) {
            resumeService.deleteResume(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(403).build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllResumes() {
        User user = getCurrentUser();
        resumeService.deleteAllResumesByUser(user);
        return ResponseEntity.ok().build();
    }
}
