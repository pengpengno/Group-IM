package com.github.im.server.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/** Server-owned group robot settings; never stored as a browser-only preference. */
@Entity
@Data
@Table(name = "conversation_bot_configs")
public class ConversationBotConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long configId;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "conversation_id", nullable = false, unique = true)
    private Conversation conversation;
    @Column(nullable = false) private boolean enabled = true;
    @Column(columnDefinition = "TEXT") private String promptTemplate;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "updated_by_user_id", nullable = false)
    private User updatedBy;
    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;
}
