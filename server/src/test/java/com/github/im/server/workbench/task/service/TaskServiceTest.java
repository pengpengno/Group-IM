package com.github.im.server.workbench.task.service;

import com.github.im.dto.workbench.task.CreateTaskRequest;
import com.github.im.dto.workbench.task.TaskActionRequest;
import com.github.im.server.workbench.common.audit.WorkbenchAuditService;
import com.github.im.server.workbench.common.context.CurrentWorkContext;
import com.github.im.server.workbench.common.integration.OrganizationAdapter;
import com.github.im.server.workbench.common.permission.WorkbenchPermission;
import com.github.im.server.workbench.common.permission.WorkbenchPermissionService;
import com.github.im.server.workbench.task.model.TaskActivityAction;
import com.github.im.server.workbench.task.model.TaskAssigneeRole;
import com.github.im.server.workbench.task.model.TaskStatus;
import com.github.im.server.workbench.task.model.TaskWorkflowAction;
import com.github.im.server.workbench.task.model.WorkTask;
import com.github.im.server.workbench.task.model.WorkTaskActivity;
import com.github.im.server.workbench.task.model.WorkTaskAssignee;
import com.github.im.server.workbench.task.repository.WorkTaskActivityRepository;
import com.github.im.server.workbench.task.repository.WorkTaskAssigneeRepository;
import com.github.im.server.workbench.task.repository.WorkTaskCommentRepository;
import com.github.im.server.workbench.task.repository.WorkTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskServiceTest {

    private WorkbenchPermissionService permissionService;
    private OrganizationAdapter organizationAdapter;
    private TaskAccessPolicy accessPolicy;
    private WorkTaskRepository taskRepository;
    private WorkTaskAssigneeRepository assigneeRepository;
    private WorkTaskCommentRepository commentRepository;
    private WorkTaskActivityRepository activityRepository;
    private WorkbenchAuditService auditService;
    private TaskService service;

    private final CurrentWorkContext context = new CurrentWorkContext(10L, "alice", 7L, "Acme", "tenant_a");

    @BeforeEach
    void setUp() {
        permissionService = mock(WorkbenchPermissionService.class);
        organizationAdapter = mock(OrganizationAdapter.class);
        accessPolicy = mock(TaskAccessPolicy.class);
        taskRepository = mock(WorkTaskRepository.class);
        assigneeRepository = mock(WorkTaskAssigneeRepository.class);
        commentRepository = mock(WorkTaskCommentRepository.class);
        activityRepository = mock(WorkTaskActivityRepository.class);
        auditService = mock(WorkbenchAuditService.class);
        service = new TaskService(
                permissionService,
                organizationAdapter,
                accessPolicy,
                new TaskStateMachine(),
                taskRepository,
                assigneeRepository,
                commentRepository,
                activityRepository,
                auditService,
                Clock.fixed(Instant.parse("2026-08-20T03:00:00Z"), ZoneOffset.UTC)
        );
        when(assigneeRepository.findByTaskIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(commentRepository.findByTaskIdAndDeletedFalseOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(activityRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createValidatesOwnerAndPersistsOwnerAssignmentActivityAndAudit() {
        when(permissionService.require(WorkbenchPermission.TASK_CREATE)).thenReturn(context);
        when(taskRepository.save(any())).thenAnswer(invocation -> {
            WorkTask task = invocation.getArgument(0);
            task.setTaskId(101L);
            task.setVersion(0L);
            return task;
        });
        when(assigneeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(new CreateTaskRequest(
                " 发布准备 ",
                "description",
                "high",
                20L,
                null,
                null,
                null,
                null
        ));

        assertEquals(101L, result.taskId());
        assertEquals("发布准备", result.title());
        assertEquals("TODO", result.status());
        assertEquals("HIGH", result.priority());
        verify(organizationAdapter).requireActiveMember(7L, 20L);
        verify(assigneeRepository).save(org.mockito.ArgumentMatchers.argThat(value ->
                value.getUserId().equals(20L) && value.getRole() == TaskAssigneeRole.OWNER));
        verify(activityRepository).save(org.mockito.ArgumentMatchers.argThat(value ->
                value.getAction() == TaskActivityAction.CREATE));
        verify(auditService).record(eq(context.tenantScope()), eq(10L), eq("TASK"), eq("CREATE"),
                eq("TASK"), eq("101"), eq(null), eq("TODO"), any());
    }

    @Test
    void startUsesStateMachineAndResourcePermission() {
        when(permissionService.require(WorkbenchPermission.TASK_UPDATE)).thenReturn(context);
        WorkTask task = new WorkTask();
        task.setTaskId(101L);
        task.setTitle("Task");
        task.setStatus(TaskStatus.TODO);
        task.setPriority(com.github.im.server.workbench.task.model.TaskPriority.MEDIUM);
        task.setCreatorId(10L);
        task.setProgress(0);
        task.setDeleted(false);
        task.setVersion(0L);
        when(taskRepository.findById(101L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        var result = service.action(101L, TaskWorkflowAction.START, new TaskActionRequest("go"));

        assertEquals("IN_PROGRESS", result.status());
        assertNotNull(result.startAt());
        verify(accessPolicy).requireWork(task, 10L);
        verify(activityRepository).save(org.mockito.ArgumentMatchers.argThat(value ->
                value.getAction() == TaskActivityAction.START
                        && value.getBeforeState() == TaskStatus.TODO
                        && value.getAfterState() == TaskStatus.IN_PROGRESS));
    }
}
