package com.recruitment.ai.controller;

import com.recruitment.ai.entity.Job;
import com.recruitment.ai.entity.MatchingScore;
import com.recruitment.ai.entity.Resume;
import com.recruitment.ai.repository.JobRepository;
import com.recruitment.ai.repository.MatchingScoreRepository;
import com.recruitment.ai.repository.ResumeRepository;
import com.recruitment.ai.service.MatchingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/screening")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ScreeningController {

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
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        // Clear previous screening results for this job and type to start a fresh "session"
        matchingScoreRepository.deleteByJob_IdAndResume_IsResumeBank(jobId, isResumeBank);

        List<Resume> relevantResumes = resumeRepository.findByJob_IdAndIsResumeBank(jobId, isResumeBank);
        List<MatchingScore> results = new ArrayList<>();

        for (Resume resume : relevantResumes) {
            MatchingScore score = matchingService.rankResumeForJob(job, resume);
            results.add(score);
        }

        return ResponseEntity.ok(results);
    }

    @GetMapping
    public List<MatchingScore> getAllResults(
            @RequestParam(value = "isResumeBank", defaultValue = "false") boolean isResumeBank) {
        return matchingScoreRepository.findByResume_IsResumeBank(isResumeBank);
    }

    @GetMapping("/results/{jobId}")
    public List<MatchingScore> getResultsByJob(
            @PathVariable Long jobId,
            @RequestParam(value = "isResumeBank", defaultValue = "false") boolean isResumeBank) {
        return matchingScoreRepository.findByJob_IdAndResume_IsResumeBankOrderByTotalScoreDesc(jobId, isResumeBank);
    }

    // Aggregate endpoints for Dashboard (combines Resume Bank + AI Screening)
    @GetMapping("/all")
    public List<MatchingScore> getAllResultsCombined() {
        return matchingScoreRepository.findAll();
    }

    @GetMapping("/all/results/{jobId}")
    public List<MatchingScore> getAllResultsByJobCombined(@PathVariable Long jobId) {
        return matchingScoreRepository.findByJob_IdOrderByTotalScoreDesc(jobId);
    }
}
