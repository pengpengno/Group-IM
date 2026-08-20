package com.github.im.server.workbench.task.service;

import com.github.im.server.workbench.common.error.WorkbenchErrorCode;
import com.github.im.server.workbench.common.error.WorkbenchException;
import com.github.im.server.workbench.task.model.TaskAssigneeRole;
import com.github.im.server.workbench.task.model.WorkTask;
import com.github.im.server.workbench.task.repository.WorkTaskAssigneeRepository;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Objects;

@Component
public class TaskAccessPolicy {

    private static final EnumSet<TaskAssigneeRole> WORK_ROLES =
            EnumSet.of(TaskAssigneeRole.OWNER, TaskAssigneeRole.COLLABORATOR);

    private final WorkTaskAssigneeRepository assigneeRepository;

    public TaskAccessPolicy(WorkTaskAssigneeRepository assigneeRepository) {
        this.assigneeRepository = assigneeRepository;
    }

    public void requireView(WorkTask task, Long userId) {
        if (!canView(task, userId)) {
            deny();
        }
    }

    public void requireManage(WorkTask task, Long userId) {
        if (!isCreatorOrOwner(task, userId)) {
            deny();
        }
    }

    public void requireWork(WorkTask task, Long userId) {
        if (isCreatorOrOwner(task, userId)) {
            return;
        }
        if (!assigneeRepository.existsByTaskIdAndUserIdAndRoleIn(task.getTaskId(), userId, WORK_ROLES)) {
            deny();
        }
    }

    public boolean canView(WorkTask task, Long userId) {
        return isCreatorOrOwner(task, userId)
                || assigneeRepository.existsByTaskIdAndUserId(task.getTaskId(), userId);
    }

    private boolean isCreatorOrOwner(WorkTask task, Long userId) {
        return Objects.equals(task.getCreatorId(), userId) || Objects.equals(task.getOwnerId(), userId);
    }

    private void deny() {
        throw WorkbenchException.forbidden(
                WorkbenchErrorCode.TASK_ACCESS_DENIED,
                "当前用户无权访问或操作该任务"
        );
    }
}
