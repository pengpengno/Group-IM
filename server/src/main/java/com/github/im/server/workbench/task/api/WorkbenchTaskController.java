package com.github.im.server.workbench.task.api;

import com.github.im.dto.workbench.task.AddTaskAssigneeRequest;
import com.github.im.dto.workbench.task.CreateTaskCommentRequest;
import com.github.im.dto.workbench.task.CreateTaskRequest;
import com.github.im.dto.workbench.task.TaskActivityDTO;
import com.github.im.dto.workbench.task.TaskActionRequest;
import com.github.im.dto.workbench.task.TaskCommentDTO;
import com.github.im.dto.workbench.task.TaskDTO;
import com.github.im.dto.workbench.task.TaskSummaryDTO;
import com.github.im.dto.workbench.task.UpdateTaskRequest;
import com.github.im.server.web.ApiResponse;
import com.github.im.server.workbench.common.error.WorkbenchErrorCode;
import com.github.im.server.workbench.common.error.WorkbenchException;
import com.github.im.server.workbench.task.model.TaskWorkflowAction;
import com.github.im.server.workbench.task.service.TaskService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/workbench/tasks")
public class WorkbenchTaskController {

    private final TaskService taskService;

    public WorkbenchTaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ApiResponse<TaskDTO> create(@RequestBody CreateTaskRequest request) {
        return ApiResponse.success(taskService.create(request));
    }

    @PatchMapping("/{taskId}")
    public ApiResponse<TaskDTO> update(@PathVariable Long taskId, @RequestBody UpdateTaskRequest request) {
        return ApiResponse.success(taskService.update(taskId, request));
    }

    @GetMapping
    public ApiResponse<List<TaskSummaryDTO>> list(@RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(taskService.list(limit));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<TaskDTO> detail(@PathVariable Long taskId) {
        return ApiResponse.success(taskService.detail(taskId));
    }

    @PostMapping("/{taskId}/actions/{action}")
    public ApiResponse<TaskDTO> action(
            @PathVariable Long taskId,
            @PathVariable String action,
            @RequestBody(required = false) TaskActionRequest request
    ) {
        return ApiResponse.success(taskService.action(taskId, parseAction(action), request));
    }

    @PostMapping("/{taskId}/assignees")
    public ApiResponse<TaskDTO> addAssignee(
            @PathVariable Long taskId,
            @RequestBody AddTaskAssigneeRequest request
    ) {
        return ApiResponse.success(taskService.addAssignee(taskId, request));
    }

    @DeleteMapping("/{taskId}/assignees/{userId}")
    public ApiResponse<TaskDTO> removeAssignee(@PathVariable Long taskId, @PathVariable Long userId) {
        return ApiResponse.success(taskService.removeAssignee(taskId, userId));
    }

    @PostMapping("/{taskId}/comments")
    public ApiResponse<TaskCommentDTO> addComment(
            @PathVariable Long taskId,
            @RequestBody CreateTaskCommentRequest request
    ) {
        return ApiResponse.success(taskService.addComment(taskId, request));
    }

    @GetMapping("/{taskId}/activities")
    public ApiResponse<List<TaskActivityDTO>> activities(@PathVariable Long taskId) {
        return ApiResponse.success(taskService.activities(taskId));
    }

    private TaskWorkflowAction parseAction(String value) {
        try {
            return TaskWorkflowAction.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw WorkbenchException.badRequest(
                    WorkbenchErrorCode.TASK_INVALID_REQUEST,
                    "不支持的 Task action: " + value
            );
        }
    }
}
