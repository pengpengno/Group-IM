package com.github.im.server.workbench.task.service;

import com.github.im.server.workbench.common.error.WorkbenchException;
import com.github.im.server.workbench.task.model.TaskStatus;
import com.github.im.server.workbench.task.model.TaskWorkflowAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskStateMachineTest {

    private final TaskStateMachine machine = new TaskStateMachine();

    @Test
    void supportsV1Transitions() {
        assertEquals(TaskStatus.IN_PROGRESS, machine.transition(TaskStatus.TODO, TaskWorkflowAction.START));
        assertEquals(TaskStatus.COMPLETED, machine.transition(TaskStatus.TODO, TaskWorkflowAction.COMPLETE));
        assertEquals(TaskStatus.CANCELLED, machine.transition(TaskStatus.TODO, TaskWorkflowAction.CANCEL));
        assertEquals(TaskStatus.BLOCKED, machine.transition(TaskStatus.IN_PROGRESS, TaskWorkflowAction.BLOCK));
        assertEquals(TaskStatus.COMPLETED, machine.transition(TaskStatus.IN_PROGRESS, TaskWorkflowAction.COMPLETE));
        assertEquals(TaskStatus.IN_PROGRESS, machine.transition(TaskStatus.BLOCKED, TaskWorkflowAction.RESUME));
        assertEquals(TaskStatus.IN_PROGRESS, machine.transition(TaskStatus.COMPLETED, TaskWorkflowAction.REOPEN));
    }

    @Test
    void cancelledIsTerminalAndInvalidTransitionsFailClosed() {
        WorkbenchException cancelled = assertThrows(
                WorkbenchException.class,
                () -> machine.transition(TaskStatus.CANCELLED, TaskWorkflowAction.START)
        );
        assertEquals("WORKBENCH_TASK_INVALID_TRANSITION", cancelled.getErrorCode());

        assertThrows(
                WorkbenchException.class,
                () -> machine.transition(TaskStatus.TODO, TaskWorkflowAction.BLOCK)
        );
    }
}
