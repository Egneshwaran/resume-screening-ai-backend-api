package com.recruitment.ai.repository;

import com.recruitment.ai.entity.MatchingScore;
import com.recruitment.ai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MatchingScoreRepository extends JpaRepository<MatchingScore, Long> {
    List<MatchingScore> findByJob_IdOrderByTotalScoreDesc(Long jobId);

    List<MatchingScore> findByUser(User user);

    List<MatchingScore> findByJob_IdAndResume_IsResumeBankOrderByTotalScoreDesc(Long jobId, boolean isResumeBank);

    @org.springframework.transaction.annotation.Transactional
    void deleteByJob_Id(Long jobId);

    @org.springframework.transaction.annotation.Transactional
    void deleteByJob_IdAndResume_IsResumeBank(Long jobId, boolean isResumeBank);

    @org.springframework.transaction.annotation.Transactional
    void deleteByResume_Id(Long resumeId);
}

