package com.github.im.server.workbench.task.service;

import com.github.im.server.workbench.common.error.WorkbenchErrorCode;
import com.github.im.server.workbench.common.error.WorkbenchException;
import com.github.im.server.workbench.task.model.TaskStatus;
import com.github.im.server.workbench.task.model.TaskWorkflowAction;
import org.springframework.stereotype.Component;

@Component
public class TaskStateMachine {

    public TaskStatus transition(TaskStatus current, TaskWorkflowAction action) {
        TaskStatus target = switch (current) {
            case TODO -> switch (action) {
                case START -> TaskStatus.IN_PROGRESS;
                case COMPLETE -> TaskStatus.COMPLETED;
                case CANCEL -> TaskStatus.CANCELLED;
                default -> null;
            };
            case IN_PROGRESS -> switch (action) {
                case BLOCK -> TaskStatus.BLOCKED;
                case COMPLETE -> TaskStatus.COMPLETED;
                case CANCEL -> TaskStatus.CANCELLED;
                default -> null;
            };
            case BLOCKED -> switch (action) {
                case RESUME -> TaskStatus.IN_PROGRESS;
                case CANCEL -> TaskStatus.CANCELLED;
                default -> null;
            };
            case COMPLETED -> action == TaskWorkflowAction.REOPEN ? TaskStatus.IN_PROGRESS : null;
            case CANCELLED -> null;
        };

        if (target == null) {
            throw WorkbenchException.conflict(
                    WorkbenchErrorCode.TASK_INVALID_TRANSITION,
                    "非法 Task 状态转换: " + current + " -> " + action
            );
        }
        return target;
    }
}
