package com.github.im.server.workbench.task.service;

import com.github.im.dto.workbench.overview.WorkbenchTaskSummaryDTO;
import com.github.im.server.workbench.task.model.TaskAssigneeRole;
import com.github.im.server.workbench.task.model.TaskStatus;
import com.github.im.server.workbench.task.repository.WorkTaskRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;

@Service
public class TaskOverviewQueryService {

    private static final EnumSet<TaskAssigneeRole> WORK_ROLES =
            EnumSet.of(TaskAssigneeRole.OWNER, TaskAssigneeRole.COLLABORATOR);
    private static final EnumSet<TaskStatus> TERMINAL_STATUSES =
            EnumSet.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED);

    private final WorkTaskRepository taskRepository;

    public TaskOverviewQueryService(WorkTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public TaskOverviewProjection query(Long userId, LocalDateTime now) {
        long assigned = taskRepository.countAssignedOpen(userId, WORK_ROLES, TERMINAL_STATUSES);
        long overdue = taskRepository.countAssignedOverdue(userId, now, WORK_ROLES, TERMINAL_STATUSES);
        var recent = taskRepository.findVisibleToUser(userId, PageRequest.of(0, 5)).stream()
                .map(task -> new WorkbenchTaskSummaryDTO(
                        task.getTaskId(),
                        task.getTitle(),
                        task.getStatus().name(),
                        task.getDueAt()
                ))
                .toList();
        return new TaskOverviewProjection(assigned, overdue, recent);
    }
}
