package com.recruitment.ai.repository;

import com.recruitment.ai.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    java.util.List<com.recruitment.ai.entity.Job> findByUser(com.recruitment.ai.entity.User user);
}
