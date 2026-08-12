package com.github.im.server.repository;

import com.github.im.server.model.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    Optional<ApprovalRequest> findByExecution_ExecutionId(Long executionId);
}
