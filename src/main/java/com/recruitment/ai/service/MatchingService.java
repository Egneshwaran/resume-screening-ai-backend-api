package com.recruitment.ai.service;

import com.recruitment.ai.entity.Job;
import com.recruitment.ai.entity.MatchingScore;
import com.recruitment.ai.entity.Resume;
import com.recruitment.ai.repository.MatchingScoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
public class MatchingService {

    @Autowired
    private MatchingScoreRepository matchingScoreRepository;

    @Value("${ai.engine.url}")
    private String AI_ENGINE_URL;

    public MatchingScore rankResumeForJob(Job job, Resume resume) {
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> request = new HashMap<>();
        request.put("job_description", job.getDescription());
        request.put("required_skills", job.getRequiredSkills());
        request.put("required_experience", job.getRequiredExperience());

        request.put("resume_text", resume.getRawText() != null ? resume.getRawText() : "");

        request.put("skill_weight", job.getSkillWeight() != null ? job.getSkillWeight() : 50);
        request.put("experience_weight", job.getExperienceWeight() != null ? job.getExperienceWeight() : 30);
        request.put("description_weight", job.getDescriptionWeight() != null ? job.getDescriptionWeight() : 20);

        try {
            ParameterizedTypeReference<Map<String, Object>> responseType = new ParameterizedTypeReference<Map<String, Object>>() {
            };

            // Ensure the URL has /process suffix if it's not already there
            String url = AI_ENGINE_URL;
            if (!url.endsWith("/process")) {
                url = url.endsWith("/") ? url + "process" : url + "/process";
            }

            Map<String, Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    responseType).getBody();

            if (response == null) {
                throw new RuntimeException("Empty response from AI engine");
            }

            MatchingScore score = MatchingScore.builder()
                    .job(job)
                    .resume(resume)
                    .totalScore(convertToDouble(response.get("total_score")))
                    .skillMatchScore(convertToDouble(response.get("skill_score")))
                    .experienceScore(convertToDouble(response.get("experience_score")))
                    .educationScore(convertToDouble(response.get("education_score")))
                    .matchedSkills((String) response.get("matched_skills"))
                    .missingSkills((String) response.get("missing_skills"))
                    .explanation((String) response.get("explanation"))
                    .status("COMPLETED")
                    .build();

            return matchingScoreRepository.save(score);
        } catch (Exception e) {
            e.printStackTrace(); 
            throw new RuntimeException("AI Screening Failed: " + e.getMessage());
        }
    }

    private Double convertToDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        try {
            return Double.valueOf(obj.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }
}
