package com.recruitment.ai.controller;

import com.recruitment.ai.entity.Job;
import com.recruitment.ai.entity.MatchingScore;
import com.recruitment.ai.entity.Resume;
import com.recruitment.ai.repository.JobRepository;
import com.recruitment.ai.repository.MatchingScoreRepository;
import com.recruitment.ai.repository.ResumeRepository;
import com.recruitment.ai.service.MatchingService;
import com.recruitment.ai.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/screening")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ScreeningController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScreeningController.class);

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private MatchingScoreRepository matchingScoreRepository;

    @Autowired
    private MatchingService matchingService;

    @PostMapping("/run/{jobId}")
    public ResponseEntity<List<MatchingScore>> runScreening(
            @PathVariable Long jobId,
            @RequestParam(value = "isResumeBank", defaultValue = "false") boolean isResumeBank) {
        logger.info("Starting screening for job ID: {}, isResumeBank: {}", jobId, isResumeBank);
        User user = getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> {
                    logger.error("Job with ID {} not found", jobId);
                    return new RuntimeException("Job not found");
                });

        if (job.getUser() != null && !job.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        // Clear previous screening results for this job and type to start a fresh
        // "session"
        matchingScoreRepository.deleteByJob_IdAndResume_IsResumeBank(jobId, isResumeBank);

        List<Resume> relevantResumes = resumeRepository.findByJob_IdAndIsResumeBank(jobId, isResumeBank);
        List<MatchingScore> results = new ArrayList<>();

        for (Resume resume : relevantResumes) {
            try {
                MatchingScore score = matchingService.rankResumeForJob(job, resume);
                results.add(score);
            } catch (Exception e) {
                logger.error("Failed to rank resume ID {}: {}", resume.getId(), e.getMessage());
            }
        }

        logger.info("Screening completed. {} results generated.", results.size());
        return ResponseEntity.ok(results);
    }

    @GetMapping
    public List<MatchingScore> getAllResults() {
        User user = getCurrentUser();
        return matchingScoreRepository.findByUser(user);
    }

    @GetMapping("/results/{jobId}")
    public ResponseEntity<List<MatchingScore>> getResultsByJob(
            @PathVariable Long jobId,
            @RequestParam(value = "isResumeBank", defaultValue = "false") boolean isResumeBank) {
        User user = getCurrentUser();
        return jobRepository.findById(jobId)
                .map(job -> {
                    if (!job.getUser().getId().equals(user.getId())) {
                        return ResponseEntity.status(403).<List<MatchingScore>>build();
                    }
                    return ResponseEntity.ok(matchingScoreRepository.findByJob_IdAndResume_IsResumeBankOrderByTotalScoreDesc(jobId, isResumeBank));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Aggregate endpoints for Dashboard (combines Resume Bank + AI Screening)
    @GetMapping("/all")
    public List<MatchingScore> getAllResultsCombined() {
        User user = getCurrentUser();
        return matchingScoreRepository.findByUser(user);
    }

    @GetMapping("/all/results/{jobId}")
    public ResponseEntity<List<MatchingScore>> getAllResultsByJobCombined(@PathVariable Long jobId) {
        User user = getCurrentUser();
        return jobRepository.findById(jobId)
                .map(job -> {
                    if (!job.getUser().getId().equals(user.getId())) {
                        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).<List<MatchingScore>>build();
                    }
                    return ResponseEntity.ok(matchingScoreRepository.findByJob_IdOrderByTotalScoreDesc(jobId));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private User getCurrentUser() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("No authentication found");
        }
        
        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @Autowired
    private com.recruitment.ai.repository.UserRepository userRepository;
}
