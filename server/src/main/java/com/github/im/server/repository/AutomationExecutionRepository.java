package com.github.im.server.repository;

import com.github.im.server.model.AutomationExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface AutomationExecutionRepository extends JpaRepository<AutomationExecution, Long> {
    Optional<AutomationExecution> findByIdempotencyKey(String idempotencyKey);
    List<AutomationExecution> findByRequestedBy_UserIdOrderByCreatedAtDesc(Long userId);
}
