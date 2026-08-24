package com.github.im.server.workbench.approval.repository;
import com.github.im.server.workbench.approval.model.ApprovalDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ApprovalDefinitionRepository extends JpaRepository<ApprovalDefinition, Long> {
    List<ApprovalDefinition> findByEnabledTrueOrderByNameAsc();
}
