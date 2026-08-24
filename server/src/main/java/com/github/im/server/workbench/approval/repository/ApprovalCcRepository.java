package com.github.im.server.workbench.approval.repository;
import com.github.im.server.workbench.approval.model.ApprovalCc;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ApprovalCcRepository extends JpaRepository<ApprovalCc, Long> {
    List<ApprovalCc> findByInstanceIdOrderByCreatedAtAsc(Long instanceId);
    boolean existsByInstanceIdAndUserId(Long instanceId, Long userId);
    List<ApprovalCc> findByUserIdOrderByCreatedAtDesc(Long userId);
}
