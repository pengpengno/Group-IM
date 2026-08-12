package com.github.im.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.im.dto.message.MessagePostRequest;
import com.github.im.enums.MessageType;
import com.github.im.server.automation.AutomationExecutionStatus;
import com.github.im.server.model.AutomationExecution;
import com.github.im.server.model.AutomationRule;
import com.github.im.server.event.MessageCreatedEvent;
import com.github.im.server.model.Message;
import com.github.im.server.repository.MessageRepository;
import com.github.im.server.repository.AutomationExecutionRepository;
import com.github.im.server.repository.AutomationRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executes v1 MESSAGE_CREATED/REPLY rules after the source message commits. */
@Service
@RequiredArgsConstructor
public class AutomationEngine {
    private final AutomationRuleRepository ruleRepository;
    private final AutomationExecutionRepository executionRepository;
    private final BotIdentityService botIdentityService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final MessageRepository messageRepository;

    @Transactional
    public void onMessageCreated(MessageCreatedEvent event) {
        if (event.conversationId() == null || isRobot(event.senderUsername())) return;
        Message source = messageRepository.findById(event.messageId()).orElse(null);
        if (source == null) return;
        for (AutomationRule rule : ruleRepository.findByConversation_ConversationIdAndEnabledTrue(event.conversationId())) {
            if (!AutomationRuleService.MESSAGE_CREATED.equals(rule.getTriggerType()) || !AutomationRuleService.REPLY.equals(rule.getActionType())) continue;
            AutomationRuleService.RuleConfig config = parse(rule.getConfiguration());
            if (config == null || !matches(config, source.getContent())) continue;
            String idempotencyKey = rule.getRuleId() + ":" + source.getMsgId();
            if (executionRepository.findByIdempotencyKey(idempotencyKey).isPresent()) continue;
            executeReply(rule, source, config, idempotencyKey);
        }
    }

    private void executeReply(AutomationRule rule, Message source, AutomationRuleService.RuleConfig config, String idempotencyKey) {
        AutomationExecution execution = new AutomationExecution();
        execution.setRule(rule);
        execution.setRequestedBy(rule.getOwner());
        execution.setConversation(source.getConversation());
        execution.setActionType(AutomationRuleService.REPLY);
        execution.setSummary("Reply to message " + source.getMsgId());
        execution.setPayload(rule.getConfiguration());
        execution.setIdempotencyKey(idempotencyKey);
        execution.setStatus(AutomationExecutionStatus.APPROVED);
        executionRepository.save(execution);
        try {
            MessagePostRequest reply = new MessagePostRequest();
            reply.setConversationId(source.getConversation().getConversationId());
            reply.setType(MessageType.TEXT);
            reply.setContent(config.replyText());
            reply.setClientMsgId("automation-" + idempotencyKey);
            messageService.sendMessage(reply, botIdentityService.assistantFor(rule.getOwner()));
            execution.setStatus(AutomationExecutionStatus.EXECUTED);
            execution.setResultSummary("Reply delivered");
        } catch (Exception ex) {
            execution.setStatus(AutomationExecutionStatus.FAILED);
            execution.setResultSummary("Reply failed");
        }
    }

    private boolean isRobot(String username) {
        return username != null && username.startsWith("ai-assistant-");
    }

    private boolean matches(AutomationRuleService.RuleConfig config, String content) {
        return config.contains() == null || config.contains().isBlank() || (content != null && content.contains(config.contains()));
    }

    private AutomationRuleService.RuleConfig parse(String configuration) {
        try { return objectMapper.readValue(configuration, AutomationRuleService.RuleConfig.class); }
        catch (Exception ignored) { return null; }
    }
}
