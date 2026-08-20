package com.github.im.server.workbench.task.service;

import com.github.im.server.workbench.common.error.WorkbenchException;
import com.github.im.server.workbench.task.model.TaskAssigneeRole;
import com.github.im.server.workbench.task.model.WorkTask;
import com.github.im.server.workbench.task.repository.WorkTaskAssigneeRepository;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskAccessPolicyTest {

    @Test
    void creatorAndOwnerCanManage() {
        WorkTaskAssigneeRepository repository = mock(WorkTaskAssigneeRepository.class);
        TaskAccessPolicy policy = new TaskAccessPolicy(repository);
        WorkTask task = task(1L, 10L, 20L);

        assertDoesNotThrow(() -> policy.requireManage(task, 10L));
        assertDoesNotThrow(() -> policy.requireManage(task, 20L));
    }

    @Test
    void collaboratorCanWorkButWatcherCannot() {
        WorkTaskAssigneeRepository repository = mock(WorkTaskAssigneeRepository.class);
        TaskAccessPolicy policy = new TaskAccessPolicy(repository);
        WorkTask task = task(1L, 10L, 20L);

        when(repository.existsByTaskIdAndUserIdAndRoleIn(eq(1L), eq(30L), any(Collection.class)))
                .thenReturn(true);
        when(repository.existsByTaskIdAndUserIdAndRoleIn(eq(1L), eq(40L), any(Collection.class)))
                .thenReturn(false);
        when(repository.existsByTaskIdAndUserId(1L, 40L)).thenReturn(true);

        assertDoesNotThrow(() -> policy.requireWork(task, 30L));
        assertDoesNotThrow(() -> policy.requireView(task, 40L));
        WorkbenchException denied = assertThrows(WorkbenchException.class, () -> policy.requireWork(task, 40L));
        assertEquals("WORKBENCH_TASK_ACCESS_DENIED", denied.getErrorCode());
    }

    private WorkTask task(Long id, Long creator, Long owner) {
        WorkTask task = new WorkTask();
        task.setTaskId(id);
        task.setCreatorId(creator);
        task.setOwnerId(owner);
        return task;
    }
}
