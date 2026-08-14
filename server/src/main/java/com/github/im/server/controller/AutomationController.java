package com.github.im.server.controller;

import com.github.im.server.automation.AutomationApprovalStatus;
import com.github.im.server.model.ApprovalRequest;
import com.github.im.server.model.User;
import com.github.im.server.service.AutomationApprovalService;
import com.github.im.server.service.AutomationRuleService;
import com.github.im.server.model.AutomationRule;
import com.github.im.server.model.AutomationExecution;
import com.github.im.server.repository.AutomationExecutionRepository;
import com.github.im.server.repository.GroupMemberRepository;
import com.github.im.server.model.ConversationMember;
import com.github.im.server.model.enums.ConversationMemberRole;
import com.github.im.enums.ConversationType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** HTTP boundary for human confirmation of sensitive robot actions. */
@RestController
@RequestMapping("/api/automation")
@RequiredArgsConstructor
public class AutomationController {
    private final AutomationApprovalService approvalService;
    private final AutomationRuleService ruleService;
    private final AutomationExecutionRepository executionRepository;
    private final GroupMemberRepository memberRepository;

    /** Only returns groups where the current actor can manage automation. */
    @GetMapping("/manageable-conversations")
    public ResponseEntity<List<ManageableConversationResponse>> manageableConversations(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(memberRepository.findByUserId(user.getUserId()).stream()
                .filter(member -> member.getConversation().getConversationType() == ConversationType.GROUP)
                .filter(member -> member.getRole() == ConversationMemberRole.OWNER || member.getRole() == ConversationMemberRole.ADMIN)
                .map(this::toManageableConversation)
                .toList());
    }

    @PostMapping("/rules")
    public ResponseEntity<RuleResponse> createRule(@RequestBody CreateReplyRuleRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(toRuleResponse(ruleService.create(user.getUserId(), request.conversationId(), request.contains(), request.replyText())));
    }

    @GetMapping("/rules")
    public ResponseEntity<List<RuleResponse>> listRules(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ruleService.list(user.getUserId()).stream().map(this::toRuleResponse).toList());
    }

    @GetMapping("/executions")
    public ResponseEntity<List<ExecutionResponse>> listExecutions(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(executionRepository.findByRequestedBy_UserIdOrderByCreatedAtDesc(user.getUserId()).stream()
                .limit(50).map(this::toExecutionResponse).toList());
    }

    @PostMapping("/rules/{ruleId}/enable")
    public ResponseEntity<RuleResponse> setRuleEnabled(@PathVariable Long ruleId, @RequestBody RuleEnabledRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(toRuleResponse(ruleService.setEnabled(user.getUserId(), ruleId, request.enabled())));
    }

    @PostMapping("/approvals/contact-lookup")
    public ResponseEntity<ApprovalResponse> requestContactLookup(
            @RequestBody ContactLookupRequest request,
            @AuthenticationPrincipal User user
    ) {
        ApprovalRequest approval = approvalService.requestContactLookup(
                user.getUserId(), request.conversationId(), request.query(), request.clientRequestId());
        return ResponseEntity.ok(toResponse(approval));
    }

    @PostMapping("/approvals/{approvalId}/decision")
    public ResponseEntity<ApprovalResponse> decide(
            @PathVariable Long approvalId,
            @RequestBody ApprovalDecisionRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(toResponse(approvalService.decide(approvalId, user.getUserId(), request.approved())));
    }

    private ApprovalResponse toResponse(ApprovalRequest approval) {
        return new ApprovalResponse(
                String.valueOf(approval.getApprovalId()),
                String.valueOf(approval.getExecution().getExecutionId()),
                approval.getExecution().getActionType(),
                approval.getExecution().getSummary(),
                approval.getStatus(),
                approval.getExpiresAt(),
                approval.getStatus() == AutomationApprovalStatus.PENDING
        );
    }

    public record ContactLookupRequest(Long conversationId, String query, String clientRequestId) { }
    public record ApprovalDecisionRequest(boolean approved) { }
    public record CreateReplyRuleRequest(Long conversationId, String contains, String replyText) { }
    public record RuleEnabledRequest(boolean enabled) { }
    public record ApprovalResponse(
            String approvalId,
            String executionId,
            String actionType,
            String summary,
            AutomationApprovalStatus status,
            LocalDateTime expiresAt,
            boolean actionable
    ) { }

    private RuleResponse toRuleResponse(AutomationRule rule) {
        return new RuleResponse(String.valueOf(rule.getRuleId()), String.valueOf(rule.getConversation().getConversationId()),
                rule.getTriggerType(), rule.getActionType(), rule.isEnabled(), rule.getConfiguration());
    }
    public record RuleResponse(String ruleId, String conversationId, String triggerType, String actionType, boolean enabled, String configuration) { }
    private ExecutionResponse toExecutionResponse(AutomationExecution execution) {
        return new ExecutionResponse(String.valueOf(execution.getExecutionId()), execution.getActionType(), execution.getSummary(),
                execution.getStatus().name(), execution.getResultSummary(), execution.getCreatedAt());
    }
    public record ExecutionResponse(String executionId, String actionType, String summary, String status, String resultSummary, LocalDateTime createdAt) { }
    private ManageableConversationResponse toManageableConversation(ConversationMember member) {
        return new ManageableConversationResponse(String.valueOf(member.getConversation().getConversationId()), member.getConversation().getGroupName());
    }
    public record ManageableConversationResponse(String conversationId, String groupName) { }
}
