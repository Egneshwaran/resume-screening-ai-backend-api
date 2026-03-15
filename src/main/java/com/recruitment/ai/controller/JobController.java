package com.recruitment.ai.controller;

import com.recruitment.ai.entity.Job;
import com.recruitment.ai.entity.User;
import com.recruitment.ai.repository.JobRepository;
import com.recruitment.ai.repository.MatchingScoreRepository;
import com.recruitment.ai.repository.ResumeRepository;
import com.recruitment.ai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JobController.class);

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private MatchingScoreRepository matchingScoreRepository;

    @Autowired
    private ResumeRepository resumeRepository;
    
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
    public List<Job> getAllJobs() {
        User user = getCurrentUser();
        return jobRepository.findByUser(user);
    }

    @PostMapping
    public Job createJob(@RequestBody Job job) {
        User user = getCurrentUser();
        job.setUser(user);
        logger.info("Creating job for user {}: {}", user.getUsername(), job.getTitle());
        return jobRepository.save(job);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        User user = getCurrentUser();
        return jobRepository.findById(id)
                .map(job -> {
                    if (job.getUser().getId().equals(user.getId())) {
                        return ResponseEntity.ok(job);
                    }
                    return ResponseEntity.status(403).<Job>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        User user = getCurrentUser();
        return jobRepository.findById(id)
                .map(job -> {
                    if (job.getUser().getId().equals(user.getId())) {
                        resumeRepository.deleteByJob_Id(id);
                        matchingScoreRepository.deleteByJob_Id(id);
                        jobRepository.deleteById(id);
                        return ResponseEntity.ok().<Void>build();
                    }
                    return ResponseEntity.status(403).<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> clearAllJobs() {
        User user = getCurrentUser();
        List<Job> userJobs = jobRepository.findByUser(user);
        for (Job job : userJobs) {
            resumeRepository.deleteByJob_Id(job.getId());
            matchingScoreRepository.deleteByJob_Id(job.getId());
            jobRepository.deleteById(job.getId());
        }
        return ResponseEntity.ok().build();
    }
}
