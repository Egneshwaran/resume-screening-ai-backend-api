package com.recruitment.ai.repository;

import com.recruitment.ai.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findTopByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);
    Optional<Subscription> findByUserId(UUID userId);
}
