package com.github.im.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.im.server.model.AutomationRule;
import com.github.im.server.model.Conversation;
import com.github.im.server.model.User;
import com.github.im.server.repository.AutomationRuleRepository;
import com.github.im.server.repository.GroupMemberRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Owns the small, declarative v1 rule contract. No user-supplied executable code is accepted. */
@Service
@RequiredArgsConstructor
public class AutomationRuleService {
    public static final String MESSAGE_CREATED = "MESSAGE_CREATED";
    public static final String REPLY = "REPLY";
    private final AutomationRuleRepository ruleRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final ConversationRoleService roleService;

    @Transactional
    public AutomationRule create(Long ownerId, Long conversationId, String contains, String replyText) {
        roleService.requireManager(conversationId, ownerId);
        if (replyText == null || replyText.isBlank()) throw new IllegalArgumentException("replyText is required");
        AutomationRule rule = new AutomationRule();
        rule.setOwner(entityManager.getReference(User.class, ownerId));
        rule.setConversation(entityManager.getReference(Conversation.class, conversationId));
        rule.setTriggerType(MESSAGE_CREATED);
        rule.setActionType(REPLY);
        rule.setEnabled(true);
        rule.setConfiguration(toJson(contains == null ? "" : contains.trim(), replyText.trim()));
        return ruleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public List<AutomationRule> list(Long ownerId) { return ruleRepository.findByOwner_UserIdOrderByUpdatedAtDesc(ownerId); }

    @Transactional
    public AutomationRule setEnabled(Long ownerId, Long ruleId, boolean enabled) {
        AutomationRule rule = ruleRepository.findById(ruleId).orElseThrow(() -> new IllegalArgumentException("Rule not found"));
        roleService.requireManager(rule.getConversation().getConversationId(), ownerId);
        rule.setEnabled(enabled);
        return rule;
    }

    private String toJson(String contains, String replyText) {
        try { return objectMapper.writeValueAsString(new RuleConfig(contains, replyText)); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Rule configuration cannot be serialized", e); }
    }

    public record RuleConfig(String contains, String replyText) { }
}
