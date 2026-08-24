package com.github.im.server.workbench.approval.api;

import com.github.im.dto.workbench.approval.*;
import com.github.im.server.web.ApiResponse;
import com.github.im.server.workbench.approval.model.ApprovalActionType;
import com.github.im.server.workbench.approval.service.ApprovalService;
import com.github.im.server.workbench.common.error.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/workbench")
public class WorkbenchApprovalController {
    private final ApprovalService service;
    public WorkbenchApprovalController(ApprovalService service) { this.service=service; }

    @GetMapping("/approval-definitions")
    public ApiResponse<List<ApprovalDefinitionDTO>> definitions() { return ApiResponse.success(service.definitions()); }
    @PostMapping("/approvals")
    public ApiResponse<ApprovalInstanceDTO> create(@RequestBody CreateApprovalRequest request) { return ApiResponse.success(service.create(request)); }
    @GetMapping("/approvals")
    public ApiResponse<List<ApprovalSummaryDTO>> list(@RequestParam(defaultValue="MY_SUBMITTED") String view,
            @RequestParam(defaultValue="50") int limit) { return ApiResponse.success(service.list(view,limit)); }
    @GetMapping("/approvals/{id}")
    public ApiResponse<ApprovalInstanceDTO> detail(@PathVariable Long id) { return ApiResponse.success(service.detail(id)); }
    @PostMapping("/approvals/{id}/actions/{action}")
    public ApiResponse<ApprovalInstanceDTO> action(@PathVariable Long id,@PathVariable String action,
            @RequestBody(required=false) ApprovalActionRequest request) {
        try { return ApiResponse.success(service.act(id,ApprovalActionType.valueOf(action.trim().toUpperCase(Locale.ROOT)),request)); }
        catch(IllegalArgumentException exception) { throw WorkbenchException.badRequest(WorkbenchErrorCode.APPROVAL_INVALID_REQUEST,"不支持的 Approval action: "+action); }
    }
}
