package com.github.im.server.workbench.approval.repository;
import com.github.im.server.workbench.approval.model.ApprovalAction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ApprovalActionRepository extends JpaRepository<ApprovalAction, Long> {
    List<ApprovalAction> findByInstanceIdOrderByCreatedAtAscActionIdAsc(Long instanceId);
    boolean existsByInstanceIdAndOperatorId(Long instanceId, Long operatorId);
    List<ApprovalAction> findByOperatorIdOrderByCreatedAtDesc(Long operatorId);
}
