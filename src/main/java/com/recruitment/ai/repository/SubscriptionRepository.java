package com.recruitment.ai.repository;

import com.recruitment.ai.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findTopByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
    Optional<Subscription> findByUserId(Long userId);
}
