package com.recruitment.ai.controller;

import com.recruitment.ai.entity.Job;
import com.recruitment.ai.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.recruitment.ai.repository.MatchingScoreRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private MatchingScoreRepository matchingScoreRepository;

    @Autowired
    private com.recruitment.ai.repository.ResumeRepository resumeRepository;

    @GetMapping
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @PostMapping
    public Job createJob(@RequestBody Job job) {
        return jobRepository.save(job);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        return jobRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> deleteJob(@PathVariable Long id) {
        return jobRepository.findById(id)
                .map(job -> {
                    matchingScoreRepository.deleteByJob_Id(id);
                    resumeRepository.deleteByJob_Id(id);
                    jobRepository.delete(job);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/clear")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> clearAllJobs() {
        matchingScoreRepository.deleteAll();
        resumeRepository.deleteAll();
        jobRepository.deleteAll();
        return ResponseEntity.ok().build();
    }
}
