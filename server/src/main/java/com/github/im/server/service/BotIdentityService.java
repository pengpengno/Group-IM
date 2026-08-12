package com.github.im.server.service;

import com.github.im.server.model.User;
import com.github.im.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Provides a durable sender identity for persisted robot replies. */
@Service
@RequiredArgsConstructor
public class BotIdentityService {
    private final UserRepository userRepository;

    @Transactional
    public User assistantFor(User requester) {
        String username = "ai-assistant-" + requester.getPrimaryCompanyId();
        return userRepository.findByUsername(username).orElseGet(() -> {
            User bot = User.builder()
                    .username(username)
                    .email(username + "@local.group")
                    .passwordHash("BOT_ACCOUNT_NOT_FOR_LOGIN")
                    .primaryCompanyId(requester.getPrimaryCompanyId())
                    .build();
            return userRepository.save(bot);
        });
    }
}
