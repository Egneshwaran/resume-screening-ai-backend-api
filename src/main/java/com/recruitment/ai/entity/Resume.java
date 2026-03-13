package com.recruitment.ai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    private String filename;
    
    private String filePath;

    private String candidateName;
    
    private String candidateEmail;

    @Column(columnDefinition = "TEXT")
    private String extractedSkills;

    private Double experienceYears;

    private String education;

    @Column(columnDefinition = "TEXT")
    private String rawText;

    @Builder.Default
    private boolean isResumeBank = false;

    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
