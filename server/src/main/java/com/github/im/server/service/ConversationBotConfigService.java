package com.github.im.server.service;

import com.github.im.enums.ConversationType;
import com.github.im.server.model.*;
import com.github.im.server.repository.ConversationBotConfigRepository;
import com.github.im.server.repository.ConversationRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class ConversationBotConfigService {
    public static final String DEFAULT_PROMPT = "你是当前群聊的智能助手，请优先回答与当前会话相关的问题，保持简洁清晰。";
    private final ConversationBotConfigRepository repository;
    private final ConversationRepository conversationRepository;
    private final EntityManager entityManager;
    private final ConversationRoleService roleService;

    @Transactional
    public ConversationBotConfig getOrCreate(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        if (conversation.getConversationType() != ConversationType.GROUP) throw new IllegalArgumentException("Bot configuration is only available for groups");
        return repository.findByConversation_ConversationId(conversationId).orElseGet(() -> {
            ConversationBotConfig config = new ConversationBotConfig(); config.setConversation(conversation); config.setEnabled(true);
            config.setPromptTemplate(DEFAULT_PROMPT); config.setUpdatedBy(conversation.getCreatedBy()); return repository.save(config);
        });
    }
    @Transactional
    public ConversationBotConfig update(Long actorId, Long conversationId, boolean enabled, String promptTemplate) {
        ConversationBotConfig config = getOrCreate(conversationId);
        roleService.requireManager(conversationId, actorId);
        config.setEnabled(enabled); config.setPromptTemplate(promptTemplate == null || promptTemplate.isBlank() ? DEFAULT_PROMPT : promptTemplate.trim());
        config.setUpdatedBy(entityManager.getReference(User.class, actorId)); return config;
    }
    @Transactional(readOnly = true)
    public void requireEnabledForGroup(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        if (conversation.getConversationType() == ConversationType.GROUP && repository.findByConversation_ConversationId(conversationId).map(ConversationBotConfig::isEnabled).orElse(true) == false)
            throw new IllegalStateException("The group robot is disabled");
    }
}
