package com.github.im.server.workbench.task.repository;

import com.github.im.server.workbench.task.model.TaskAssigneeRole;
import com.github.im.server.workbench.task.model.WorkTaskAssignee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkTaskAssigneeRepository extends JpaRepository<WorkTaskAssignee, Long> {

    List<WorkTaskAssignee> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    boolean existsByTaskIdAndUserId(Long taskId, Long userId);

    boolean existsByTaskIdAndUserIdAndRole(Long taskId, Long userId, TaskAssigneeRole role);

    void deleteByTaskIdAndUserId(Long taskId, Long userId);

    void deleteByTaskIdAndRole(Long taskId, TaskAssigneeRole role);
}
