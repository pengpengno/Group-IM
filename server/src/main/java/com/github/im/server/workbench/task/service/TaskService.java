package com.github.im.server.workbench.task.service;

import com.github.im.dto.workbench.task.AddTaskAssigneeRequest;
import com.github.im.dto.workbench.task.CreateTaskCommentRequest;
import com.github.im.dto.workbench.task.CreateTaskRequest;
import com.github.im.dto.workbench.task.TaskActivityDTO;
import com.github.im.dto.workbench.task.TaskActionRequest;
import com.github.im.dto.workbench.task.TaskAssigneeDTO;
import com.github.im.dto.workbench.task.TaskCommentDTO;
import com.github.im.dto.workbench.task.TaskDTO;
import com.github.im.dto.workbench.task.TaskSummaryDTO;
import com.github.im.dto.workbench.task.UpdateTaskRequest;
import com.github.im.server.workbench.common.audit.WorkbenchAuditService;
import com.github.im.server.workbench.common.context.CurrentWorkContext;
import com.github.im.server.workbench.common.error.WorkbenchErrorCode;
import com.github.im.server.workbench.common.error.WorkbenchException;
import com.github.im.server.workbench.common.integration.OrganizationAdapter;
import com.github.im.server.workbench.common.permission.WorkbenchPermission;
import com.github.im.server.workbench.common.permission.WorkbenchPermissionService;
import com.github.im.server.workbench.task.model.TaskActivityAction;
import com.github.im.server.workbench.task.model.TaskAssigneeRole;
import com.github.im.server.workbench.task.model.TaskPriority;
import com.github.im.server.workbench.task.model.TaskStatus;
import com.github.im.server.workbench.task.model.TaskWorkflowAction;
import com.github.im.server.workbench.task.model.WorkTask;
import com.github.im.server.workbench.task.model.WorkTaskActivity;
import com.github.im.server.workbench.task.model.WorkTaskAssignee;
import com.github.im.server.workbench.task.model.WorkTaskComment;
import com.github.im.server.workbench.task.repository.WorkTaskActivityRepository;
import com.github.im.server.workbench.task.repository.WorkTaskAssigneeRepository;
import com.github.im.server.workbench.task.repository.WorkTaskCommentRepository;
import com.github.im.server.workbench.task.repository.WorkTaskRepository;
import com.github.im.server.workbench.task.event.TaskNotificationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class TaskService {

    private final WorkbenchPermissionService permissionService;
    private final OrganizationAdapter organizationAdapter;
    private final TaskAccessPolicy accessPolicy;
    private final TaskStateMachine stateMachine;
    private final WorkTaskRepository taskRepository;
    private final WorkTaskAssigneeRepository assigneeRepository;
    private final WorkTaskCommentRepository commentRepository;
    private final WorkTaskActivityRepository activityRepository;
    private final WorkbenchAuditService auditService;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public TaskService(
            WorkbenchPermissionService permissionService,
            OrganizationAdapter organizationAdapter,
            TaskAccessPolicy accessPolicy,
            TaskStateMachine stateMachine,
            WorkTaskRepository taskRepository,
            WorkTaskAssigneeRepository assigneeRepository,
            WorkTaskCommentRepository commentRepository,
            WorkTaskActivityRepository activityRepository,
            WorkbenchAuditService auditService,
            Clock workbenchClock,
            ApplicationEventPublisher eventPublisher
    ) {
        this.permissionService = permissionService;
        this.organizationAdapter = organizationAdapter;
        this.accessPolicy = accessPolicy;
        this.stateMachine = stateMachine;
        this.taskRepository = taskRepository;
        this.assigneeRepository = assigneeRepository;
        this.commentRepository = commentRepository;
        this.activityRepository = activityRepository;
        this.auditService = auditService;
        this.clock = workbenchClock;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TaskDTO create(CreateTaskRequest request) {
        CurrentWorkContext context = permissionService.require(WorkbenchPermission.TASK_CREATE);
        requireRequest(request);
        String title = requireTitle(request.title());
        TaskPriority priority = parsePriority(request.priority(), TaskPriority.MEDIUM);

        if (request.ownerId() != null) {
            organizationAdapter.requireActiveMember(context.companyId(), request.ownerId());
        }

        WorkTask task = WorkTask.builder()
                .title(title)
                .description(normalizeOptionalText(request.description()))
                .status(TaskStatus.TODO)
                .priority(priority)
                .creatorId(context.userId())
                .ownerId(request.ownerId())
                .departmentId(request.departmentId())
                .conversationId(request.conversationId())
                .startAt(request.startAt())
                .dueAt(request.dueAt())
                .progress(0)
                .deleted(false)
                .build();
        task = taskRepository.save(task);

        if (request.ownerId() != null) {
            assigneeRepository.save(WorkTaskAssignee.builder()
                    .taskId(task.getTaskId())
                    .userId(request.ownerId())
                    .role(TaskAssigneeRole.OWNER)
                    .build());
        }

        recordActivity(task, context.userId(), TaskActivityAction.CREATE, null, TaskStatus.TODO, null);
        audit(context, task, TaskActivityAction.CREATE, null, TaskStatus.TODO, Map.of());
        return toDto(task);
    }

    @Transactional
    public TaskDTO update(Long taskId, UpdateTaskRequest request) {
        CurrentWorkContext context = permissionService.require(WorkbenchPermission.TASK_UPDATE);
        requireRequest(request);
        WorkTask task = requireTask(taskId);
        accessPolicy.requireManage(task, context.userId());

        boolean changed = false;
        if (request.title() != null) {
            task.setTitle(requireTitle(request.title()));
            changed = true;
        }
        if (request.description() != null) {
            task.setDescription(normalizeOptionalText(request.description()));
            changed = true;
        }
        if (request.priority() != null) {
            task.setPriority(parsePriority(request.priority(), null));
            changed = true;
        }
        if (request.dueAt() != null) {
            task.setDueAt(request.dueAt());
            changed = true;
        }
        if (request.progress() != null) {
            int progress = request.progress();
            if (progress < 0 || progress > 100) {
                badRequest("progress 必须在 0..100 之间");
            }
            if (task.getStatus() == TaskStatus.COMPLETED && progress != 100) {
                badRequest("已完成任务的 progress 必须保持 100");
            }
            task.setProgress(progress);
            changed = true;
        }

        if (changed) {
            task = taskRepository.save(task);
            recordActivity(task, context.userId(), TaskActivityAction.UPDATE, task.getStatus(), task.getStatus(), null);
            audit(context, task, TaskActivityAction.UPDATE, task.getStatus(), task.getStatus(), Map.of());
        }
        return toDto(task);
    }

    @Transactional(readOnly = true)
    public List<TaskSummaryDTO> list(int requestedLimit) {
        CurrentWorkContext context = permissionService.require(WorkbenchPermission.VIEW_WORKBENCH);
        int limit = Math.max(1, Math.min(requestedLimit, 100));
        return taskRepository.findVisibleToUser(context.userId(), PageRequest.of(0, limit)).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskDTO detail(Long taskId) {
        CurrentWorkContext context = permissionService.require(WorkbenchPermission.VIEW_WORKBENCH);
        WorkTask task = requireTask(taskId);
        accessPolicy.requireView(task, context.userId());
        return toDto(task);
    }

    @Transactional
    public TaskDTO action(Long taskId, TaskWorkflowAction action, TaskActionRequest request) {
        CurrentWorkContext context = permissionService.require(WorkbenchPermission.TASK_UPDATE);
        WorkTask task = requireTask(taskId);
        if (action == TaskWorkflowAction.REOPEN || action == TaskWorkflowAction.CANCEL) {
            accessPolicy.requireManage(task, context.userId());
        } else {
            accessPolicy.requireWork(task, context.userId());
        }

        TaskStatus before = task.getStatus();
        TaskStatus after = stateMachine.transition(before, action);
        LocalDateTime now = LocalDateTime.now(clock);
        task.setStatus(after);
        if (after == TaskStatus.IN_PROGRESS && task.getStartAt() == null) {
            task.setStartAt(now);
        }
        if (after == TaskStatus.COMPLETED) {
            task.setCompletedAt(now);
            task.setProgress(100);
        }
        if (action == TaskWorkflowAction.REOPEN || action == TaskWorkflowAction.CANCEL) {
            task.setCompletedAt(null);
        }
        task = taskRepository.save(task);

        String note = normalizeNote(request == null ? null : request.note());
        TaskActivityAction activityAction = TaskActivityAction.valueOf(action.name());
        recordActivity(task, context.userId(), activityAction, before, after, note);
        audit(context, task, activityAction, before, after, Map.of());
        if (activityAction == TaskActivityAction.COMPLETE || activityAction == TaskActivityAction.REOPEN) {
            List<Long> receivers = assigneeRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                    .map(WorkTaskAssignee::getUserId)
                    .toList();
            publishNotification(context, task, activityAction, receivers);
        }
        return toDto(task);
    }

    @Transactional
    public TaskDTO addAssignee(Long taskId, AddTaskAssigneeRequest request) {
        CurrentWorkContext context = permissionService.require(WorkbenchPermission.TASK_UPDATE);
        requireRequest(request);
        if (request.userId() == null) {
            badRequest("assignee userId 不能为空");
        }
        WorkTask task = requireTask(taskId);
        accessPolicy.requireManage(task, context.userId());
        organizationAdapter.requireActiveMember(context.companyId(), request.userId());
        TaskAssigneeRole role = parseRole(request.role());

        if (role == TaskAssigneeRole.OWNER) {
            assigneeRepository.deleteByTaskIdAndRole(taskId, TaskAssigneeRole.OWNER);
            task.setOwnerId(request.userId());
            task = taskRepository.save(task);
        }

        if (!assigneeRepository.existsByTaskIdAndUserIdAndRole(taskId, request.userId(), role)) {
            assigneeRepository.save(WorkTaskAssignee.builder()
                    .taskId(taskId)
                    .userId(request.userId())
                    .role(role)
                    .build());
            String detail = "userId=" + request.userId() + ", role=" + role;
            recordActivity(task, context.userId(), TaskActivityAction.ASSIGN, task.getStatus(), task.getStatus(), detail);
            audit(context, task, TaskActivityAction.ASSIGN, task.getStatus(), task.getStatus(),
                    Map.of("userId", request.userId(), "role", role.name()));
            publishNotification(context, task, TaskActivityAction.ASSIGN, List.of(request.userId()));
        }
        return toDto(task);
    }

    @Transactional
    public TaskDTO removeAssignee(Long taskId, Long userId) {
        CurrentWorkContext context = permissionService.require(WorkbenchPermission.TASK_UPDATE);
        WorkTask task = requireTask(taskId);
        accessPolicy.requireManage(task, context.userId());
        List<WorkTaskAssignee> existing = assigneeRepository.findByTaskIdAndUserId(taskId, userId);
        if (existing.isEmpty()) {
            throw WorkbenchException.notFound(
                    WorkbenchErrorCode.TASK_ASSIGNEE_INVALID,
                    "任务中不存在该 assignee"
            );
        }

        assigneeRepository.deleteByTaskIdAndUserId(taskId, userId);
        if (Objects.equals(task.getOwnerId(), userId)) {
            task.setOwnerId(null);
            task = taskRepository.save(task);
        }
        String detail = "userId=" + userId;
        recordActivity(task, context.userId(), TaskActivityAction.UNASSIGN, task.getStatus(), task.getStatus(), detail);
        audit(context, task, TaskActivityAction.UNASSIGN, task.getStatus(), task.getStatus(), Map.of("userId", userId));
        return toDto(task);
    }

    @Transactional
    public TaskCommentDTO addComment(Long taskId, CreateTaskCommentRequest request) {
        CurrentWorkContext context = permissionService.require(WorkbenchPermission.VIEW_WORKBENCH);
        requireRequest(request);
        WorkTask task = requireTask(taskId);
        accessPolicy.requireView(task, context.userId());

        String content = normalizeComment(request.content());
        if (request.replyToId() != null) {
            commentRepository.findByCommentIdAndTaskIdAndDeletedFalse(request.replyToId(), taskId)
                    .orElseThrow(() -> WorkbenchException.badRequest(
                            WorkbenchErrorCode.TASK_COMMENT_INVALID,
                            "replyToId 不属于当前任务或已删除"
                    ));
        }

        WorkTaskComment comment = commentRepository.save(WorkTaskComment.builder()
                .taskId(taskId)
                .authorId(context.userId())
                .content(content)
                .replyToId(request.replyToId())
                .deleted(false)
                .build());
        recordActivity(task, context.userId(), TaskActivityAction.COMMENT, task.getStatus(), task.getStatus(),
                "commentId=" + comment.getCommentId());
        audit(context, task, TaskActivityAction.COMMENT, task.getStatus(), task.getStatus(),
                Map.of("commentId", comment.getCommentId()));
        return toComment(comment);
    }

    @Transactional(readOnly = true)
    public List<TaskActivityDTO> activities(Long taskId) {
        CurrentWorkContext context = permissionService.require(WorkbenchPermission.VIEW_WORKBENCH);
        WorkTask task = requireTask(taskId);
        accessPolicy.requireView(task, context.userId());
        return activityRepository.findByTaskIdOrderByCreatedAtAscActivityIdAsc(taskId).stream()
                .map(this::toActivity)
                .toList();
    }

    private WorkTask requireTask(Long taskId) {
        if (taskId == null) {
            badRequest("taskId 不能为空");
        }
        return taskRepository.findById(taskId)
                .filter(task -> !Boolean.TRUE.equals(task.getDeleted()))
                .orElseThrow(() -> WorkbenchException.notFound(
                        WorkbenchErrorCode.TASK_NOT_FOUND,
                        "任务不存在: " + taskId
                ));
    }

    private TaskDTO toDto(WorkTask task) {
        List<TaskAssigneeDTO> assignees = assigneeRepository.findByTaskIdOrderByCreatedAtAsc(task.getTaskId()).stream()
                .map(value -> new TaskAssigneeDTO(value.getUserId(), value.getRole().name(), value.getCreatedAt()))
                .toList();
        List<TaskCommentDTO> comments = commentRepository
                .findByTaskIdAndDeletedFalseOrderByCreatedAtAsc(task.getTaskId()).stream()
                .map(this::toComment)
                .toList();
        return new TaskDTO(
                task.getTaskId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getPriority().name(),
                task.getCreatorId(),
                task.getOwnerId(),
                task.getDepartmentId(),
                task.getConversationId(),
                task.getStartAt(),
                task.getDueAt(),
                task.getCompletedAt(),
                task.getProgress(),
                task.getVersion() == null ? 0L : task.getVersion(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                assignees,
                comments
        );
    }

    private TaskSummaryDTO toSummary(WorkTask task) {
        return new TaskSummaryDTO(
                task.getTaskId(),
                task.getTitle(),
                task.getStatus().name(),
                task.getPriority().name(),
                task.getOwnerId(),
                task.getDueAt(),
                task.getProgress(),
                task.getUpdatedAt()
        );
    }

    private TaskCommentDTO toComment(WorkTaskComment comment) {
        return new TaskCommentDTO(
                comment.getCommentId(),
                comment.getAuthorId(),
                comment.getContent(),
                comment.getReplyToId(),
                comment.getCreatedAt()
        );
    }

    private TaskActivityDTO toActivity(WorkTaskActivity activity) {
        return new TaskActivityDTO(
                activity.getActivityId(),
                activity.getActorId(),
                activity.getAction().name(),
                activity.getBeforeState() == null ? null : activity.getBeforeState().name(),
                activity.getAfterState() == null ? null : activity.getAfterState().name(),
                activity.getDetail(),
                activity.getCreatedAt()
        );
    }

    private void recordActivity(
            WorkTask task,
            Long actorId,
            TaskActivityAction action,
            TaskStatus before,
            TaskStatus after,
            String detail
    ) {
        activityRepository.save(WorkTaskActivity.builder()
                .taskId(task.getTaskId())
                .actorId(actorId)
                .action(action)
                .beforeState(before)
                .afterState(after)
                .detail(detail)
                .build());
    }

    private void audit(
            CurrentWorkContext context,
            WorkTask task,
            TaskActivityAction action,
            TaskStatus before,
            TaskStatus after,
            Map<String, Object> metadata
    ) {
        auditService.record(
                context.tenantScope(),
                context.userId(),
                "TASK",
                action.name(),
                "TASK",
                String.valueOf(task.getTaskId()),
                before == null ? null : before.name(),
                after == null ? null : after.name(),
                metadata
        );
    }

    private void requireRequest(Object request) {
        if (request == null) {
            badRequest("请求体不能为空");
        }
    }

    private void publishNotification(CurrentWorkContext context, WorkTask task,
                                     TaskActivityAction action, List<Long> receiverIds) {
        List<Long> recipients = receiverIds.stream()
                .filter(Objects::nonNull)
                .filter(userId -> !Objects.equals(userId, context.userId()))
                .distinct()
                .toList();
        if (recipients.isEmpty()) return;
        eventPublisher.publishEvent(new TaskNotificationEvent(
                UUID.randomUUID().toString(), context.companyId(), task.getTaskId(),
                task.getTitle(), task.getStatus().name(), action, context.userId(),
                recipients, task.getConversationId(), Instant.now(clock)));
    }

    private String requireTitle(String value) {
        if (value == null || value.isBlank()) {
            badRequest("任务标题不能为空");
        }
        String normalized = value.trim();
        if (normalized.length() > 200) {
            badRequest("任务标题不能超过 200 个字符");
        }
        return normalized;
    }

    private TaskPriority parsePriority(String value, TaskPriority defaultValue) {
        if (value == null || value.isBlank()) {
            if (defaultValue != null) {
                return defaultValue;
            }
            badRequest("priority 不能为空");
        }
        try {
            return TaskPriority.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            badRequest("不支持的 priority: " + value);
            throw exception;
        }
    }

    private TaskAssigneeRole parseRole(String value) {
        if (value == null || value.isBlank()) {
            return TaskAssigneeRole.COLLABORATOR;
        }
        try {
            return TaskAssigneeRole.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw WorkbenchException.badRequest(
                    WorkbenchErrorCode.TASK_ASSIGNEE_INVALID,
                    "不支持的 assignee role: " + value
            );
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeNote(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > 1000) {
            badRequest("action note 不能超过 1000 个字符");
        }
        return normalized;
    }

    private String normalizeComment(String value) {
        if (value == null || value.isBlank()) {
            throw WorkbenchException.badRequest(
                    WorkbenchErrorCode.TASK_COMMENT_INVALID,
                    "评论内容不能为空"
            );
        }
        String normalized = value.trim();
        if (normalized.length() > 4000) {
            throw WorkbenchException.badRequest(
                    WorkbenchErrorCode.TASK_COMMENT_INVALID,
                    "评论内容不能超过 4000 个字符"
            );
        }
        return normalized;
    }

    private void badRequest(String message) {
        throw new WorkbenchException(HttpStatus.BAD_REQUEST, WorkbenchErrorCode.TASK_INVALID_REQUEST, message);
    }
}
