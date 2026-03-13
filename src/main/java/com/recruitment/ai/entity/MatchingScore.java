package com.recruitment.ai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "matching_scores")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne
    @JoinColumn(name = "resume_id")
    private Resume resume;

    private Double totalScore; // 0 to 100

    private Double skillMatchScore;

    private Double experienceScore;

    private Double educationScore;

    @Column(columnDefinition = "TEXT")
    private String matchedSkills;

    @Column(columnDefinition = "TEXT")
    private String missingSkills;

    @Column(columnDefinition = "TEXT")
    private String explanation; // For XAI

    private String status; // SHORTLISTED, REJECTED, PENDING
}
