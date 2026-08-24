package com.github.im.server.workbench.approval.repository;
import com.github.im.server.workbench.approval.model.ApprovalInstance;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ApprovalInstanceRepository extends JpaRepository<ApprovalInstance, Long> {
    List<ApprovalInstance> findByApplicantIdOrderByCreatedAtDesc(Long applicantId, Pageable pageable);
}
