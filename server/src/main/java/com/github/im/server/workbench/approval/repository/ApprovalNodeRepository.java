package com.github.im.server.workbench.approval.repository;
import com.github.im.server.workbench.approval.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ApprovalNodeRepository extends JpaRepository<ApprovalNode, Long> {
    List<ApprovalNode> findByInstanceIdOrderByNodeOrderAsc(Long instanceId);
    Optional<ApprovalNode> findByInstanceIdAndNodeOrder(Long instanceId, Integer nodeOrder);
    boolean existsByInstanceIdAndAssigneeId(Long instanceId, Long assigneeId);
    List<ApprovalNode> findByAssigneeIdAndStatusOrderByStartedAtDesc(Long assigneeId, ApprovalNodeStatus status);
    long countByAssigneeIdAndStatus(Long assigneeId, ApprovalNodeStatus status);
}
