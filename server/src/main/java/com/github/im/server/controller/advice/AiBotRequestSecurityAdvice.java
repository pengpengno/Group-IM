package com.github.im.server.controller.advice;

import com.github.im.dto.message.MessageDTO;
import com.github.im.server.controller.AiBotController;
import com.github.im.server.model.User;
import com.github.im.server.repository.GroupMemberRepository;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;

/**
 * Enforces server-side identity for authenticated AI bot requests.
 *
 * The client supplied fromAccountId is never trusted. For persisted IM
 * conversations the caller must also be a conversation member. Negative
 * conversation IDs are reserved for the local assistant conversation and do
 * not have a server-side Conversation row yet.
 */
@ControllerAdvice(assignableTypes = AiBotController.class)
public class AiBotRequestSecurityAdvice extends RequestBodyAdviceAdapter {

    private final GroupMemberRepository groupMemberRepository;

    public AiBotRequestSecurityAdvice(GroupMemberRepository groupMemberRepository) {
        this.groupMemberRepository = groupMemberRepository;
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return MessageDTO.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public Object afterBodyRead(
            Object body,
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        if (!(body instanceof MessageDTO<?> message)) {
            return body;
        }

        User authenticatedUser = currentUser();
        message.setFromAccountId(authenticatedUser.getUserId());

        Long conversationId = message.getConversationId();
        if (conversationId == null || conversationId == 0L) {
            throw new IllegalArgumentException("conversationId is required");
        }

        if (conversationId > 0L && groupMemberRepository
                .findByConversationIdAndUserId(conversationId, authenticatedUser.getUserId())
                .isEmpty()) {
            throw new SecurityException("You are not a member of this conversation");
        }

        return message;
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User user)) {
            throw new SecurityException("Authentication is required for AI bot requests");
        }
        return user;
    }
}
