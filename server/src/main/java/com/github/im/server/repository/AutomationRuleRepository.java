package com.github.im.server.repository;

import com.github.im.server.model.AutomationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AutomationRuleRepository extends JpaRepository<AutomationRule, Long> {
    List<AutomationRule> findByOwner_UserIdOrderByUpdatedAtDesc(Long userId);
    List<AutomationRule> findByConversation_ConversationIdAndEnabledTrue(Long conversationId);
}
