package com.recruitment.ai.repository;

import com.recruitment.ai.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByJob_Id(Long jobId);
    List<Resume> findByJob_IdAndIsResumeBank(Long jobId, boolean isResumeBank);
    List<Resume> findByIsResumeBank(boolean isResumeBank);

    @org.springframework.transaction.annotation.Transactional
    void deleteByJob_Id(Long jobId);
}
