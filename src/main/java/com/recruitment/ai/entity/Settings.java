package com.recruitment.ai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // 1. Screening Threshold Settings
    @Builder.Default
    private int highPotentialThreshold = 85;
    @Builder.Default
    private int mediumMatchThreshold = 70;
    @Builder.Default
    private int lowMatchThreshold = 50;

    // 2. Resume Parsing Settings
    @Builder.Default
    private boolean autoParseResumes = true;
    @Builder.Default
    private boolean extractSkills = true;
    @Builder.Default
    private boolean detectExperience = true;
    @Builder.Default
    private boolean extractEducation = true;

    // 3. Smart Candidate Alerts
    @Builder.Default
    private boolean alertScoreThresholdEnabled = true;
    @Builder.Default
    private boolean alertStrongSkillMatch = false;
    @Builder.Default
    private boolean alertTopCandidateIdentified = true;

    // 4. Candidate Communication Settings
    @Builder.Default
    private boolean autoEmailShortlisted = false;

    @Column(length = 2000)
    @Builder.Default
    private String defaultEmailTemplate = "Dear {candidate_name},\n\nWe are pleased to inform you that your profile has been shortlisted for the {job_title} position.\n\nBest regards,\nHR Team";

    @Column(length = 1000)
    @Builder.Default
    private String recruiterSignature = "Recruiter\nTalent Acquisition Team";
}
