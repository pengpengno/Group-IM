package com.github.im.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.im.server.automation.AutomationApprovalStatus;
import com.github.im.server.automation.AutomationExecutionStatus;
import com.github.im.server.model.ApprovalRequest;
import com.github.im.server.model.AutomationExecution;
import com.github.im.server.model.Conversation;
import com.github.im.server.model.User;
import com.github.im.server.repository.ApprovalRequestRepository;
import com.github.im.server.repository.AutomationExecutionRepository;
import com.github.im.server.repository.GroupMemberRepository;
import com.github.im.server.ai.mcp.BotToolContext;
import com.github.im.server.ai.mcp.InternalMcpToolGateway;
import com.github.im.server.ai.mcp.McpToolInvocation;
import com.github.im.server.ai.mcp.McpToolResult;
import com.github.im.dto.message.MessageDTO;
import com.github.im.dto.message.MessagePayLoad;
import com.github.im.dto.message.MessagePostRequest;
import com.github.im.enums.MessageType;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/** Creates and decides sensitive automation intents without executing them implicitly. */
@Service
@RequiredArgsConstructor
public class AutomationApprovalService {
    public static final String CONTACT_LOOKUP_ACTION = "CONTACT_LOOKUP";

    private final AutomationExecutionRepository executionRepository;
    private final ApprovalRequestRepository approvalRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;
    private final InternalMcpToolGateway mcpToolGateway;
    private final MessageService messageService;
    private final BotIdentityService botIdentityService;

    @Transactional
    public ApprovalRequest requestContactLookup(Long actorUserId, Long conversationId, String query, String clientRequestId) {
        requireConversationAccess(actorUserId, conversationId);
        String idempotencyKey = "contact-lookup:" + actorUserId + ":" + safeRequestId(clientRequestId);
        return executionRepository.findByIdempotencyKey(idempotencyKey)
                .map(existing -> approvalRepository.findByExecution_ExecutionId(existing.getExecutionId())
                        .orElseThrow(() -> new IllegalStateException("Approval record is missing")))
                .orElseGet(() -> createContactLookup(actorUserId, conversationId, query, idempotencyKey));
    }

    @Transactional
    public ApprovalRequest decide(Long approvalId, Long actorUserId, boolean approved) {
        ApprovalRequest approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("Approval request does not exist"));
        if (!approval.getRequestedBy().getUserId().equals(actorUserId)) {
            throw new SecurityException("Only the requester can decide this approval");
        }
        if (approval.getStatus() != AutomationApprovalStatus.PENDING) {
            return approval;
        }
        if (approval.getExpiresAt().isBefore(LocalDateTime.now())) {
            approval.setStatus(AutomationApprovalStatus.DECLINED);
            approval.getExecution().setStatus(AutomationExecutionStatus.DECLINED);
            return approval;
        }

        approval.setStatus(approved ? AutomationApprovalStatus.APPROVED : AutomationApprovalStatus.DECLINED);
        approval.setDecidedAt(LocalDateTime.now());
        AutomationExecution execution = approval.getExecution();
        execution.setStatus(approved ? AutomationExecutionStatus.APPROVED : AutomationExecutionStatus.DECLINED);
        if (approved && CONTACT_LOOKUP_ACTION.equals(execution.getActionType())) {
            McpToolResult result = mcpToolGateway.invoke(new McpToolInvocation(
                    "directory.contact.lookup", Map.of("query", queryFrom(execution.getPayload())),
                    new BotToolContext(actorUserId,
                            execution.getConversation() == null ? null : execution.getConversation().getConversationId(),
                            null, "approval-" + approvalId, true)));
            execution.setStatus(result.isSuccess() ? AutomationExecutionStatus.EXECUTED : AutomationExecutionStatus.FAILED);
            execution.setResultSummary(result.isSuccess() ? "Sensitive contact lookup completed" : result.errorCode());
            if (result.isSuccess()) {
                persistResultMessage(execution, actorUserId, result.displayText(), approvalId);
            }
        }
        return approval;
    }

    private ApprovalRequest createContactLookup(Long actorUserId, Long conversationId, String query, String idempotencyKey) {
        AutomationExecution execution = new AutomationExecution();
        execution.setRequestedBy(entityManager.getReference(User.class, actorUserId));
        if (conversationId != null && conversationId > 0) {
            execution.setConversation(entityManager.getReference(Conversation.class, conversationId));
        }
        execution.setActionType(CONTACT_LOOKUP_ACTION);
        execution.setSummary("查看联系人“" + normalizedQuery(query) + "”的敏感联系方式");
        execution.setPayload(asJson(Map.of("query", normalizedQuery(query))));
        execution.setIdempotencyKey(idempotencyKey);
        execution.setStatus(AutomationExecutionStatus.PENDING_APPROVAL);
        executionRepository.save(execution);

        ApprovalRequest approval = new ApprovalRequest();
        approval.setExecution(execution);
        approval.setRequestedBy(execution.getRequestedBy());
        approval.setStatus(AutomationApprovalStatus.PENDING);
        approval.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        return approvalRepository.save(approval);
    }

    private void requireConversationAccess(Long actorUserId, Long conversationId) {
        if (conversationId == null || conversationId <= 0) return;
        if (groupMemberRepository.findByConversationIdAndUserId(conversationId, actorUserId).isEmpty()) {
            throw new SecurityException("You are not a member of this conversation");
        }
    }

    private String safeRequestId(String clientRequestId) {
        return clientRequestId == null || clientRequestId.isBlank() ? UUID.randomUUID().toString() : clientRequestId.trim();
    }

    private String normalizedQuery(String query) {
        String normalized = query == null ? "" : query.trim();
        return normalized.isEmpty() ? "未指定联系人" : normalized.substring(0, Math.min(normalized.length(), 120));
    }

    private String asJson(Map<String, String> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize automation payload", exception);
        }
    }

    private String queryFrom(String payload) {
        try { return objectMapper.readTree(payload).path("query").asText(""); }
        catch (Exception ignored) { return ""; }
    }

    private void persistResultMessage(AutomationExecution execution, Long actorUserId, String content, Long approvalId) {
        if (execution.getConversation() == null || execution.getResultMessageId() != null) {
            return;
        }
        MessagePostRequest message = new MessagePostRequest();
        message.setConversationId(execution.getConversation().getConversationId());
        message.setType(MessageType.TEXT);
        message.setContent(content);
        message.setClientMsgId("approval-result-" + approvalId);
        MessageDTO<MessagePayLoad> saved = messageService.sendMessage(message,
                botIdentityService.assistantFor(entityManager.getReference(User.class, actorUserId)));
        execution.setResultMessageId(saved.getMsgId());
    }
}
