package com.github.im.server.repository;
import com.github.im.server.model.ConversationBotConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ConversationBotConfigRepository extends JpaRepository<ConversationBotConfig, Long> {
    Optional<ConversationBotConfig> findByConversation_ConversationId(Long conversationId);
}
