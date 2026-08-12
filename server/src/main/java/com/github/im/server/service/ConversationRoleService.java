package com.github.im.server.service;

import com.github.im.enums.ConversationType;
import com.github.im.server.model.Conversation;
import com.github.im.server.model.ConversationMember;
import com.github.im.server.model.enums.ConversationMemberRole;
import com.github.im.server.repository.ConversationRepository;
import com.github.im.server.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class ConversationRoleService {
    private final GroupMemberRepository memberRepository;
    private final ConversationRepository conversationRepository;

    @Transactional(readOnly = true)
    public void requireManager(Long conversationId, Long actorId) {
        ConversationMember member = requireMember(conversationId, actorId);
        if (member.getRole() != ConversationMemberRole.OWNER && member.getRole() != ConversationMemberRole.ADMIN)
            throw new SecurityException("Group administrator permission is required");
    }
    @Transactional(readOnly = true)
    public void requireOwner(Long conversationId, Long actorId) {
        if (requireMember(conversationId, actorId).getRole() != ConversationMemberRole.OWNER)
            throw new SecurityException("Group owner permission is required");
    }
    @Transactional
    public ConversationMember updateRole(Long conversationId, Long actorId, Long targetUserId, ConversationMemberRole role) {
        requireOwner(conversationId, actorId);
        if (role == ConversationMemberRole.OWNER) throw new IllegalArgumentException("Use ownership transfer for OWNER");
        ConversationMember target = requireMember(conversationId, targetUserId);
        if (target.getRole() == ConversationMemberRole.OWNER) throw new IllegalArgumentException("Transfer ownership before changing the owner role");
        target.setRole(role); return target;
    }
    @Transactional
    public void transferOwnership(Long conversationId, Long actorId, Long targetUserId) {
        requireOwner(conversationId, actorId);
        ConversationMember target = requireMember(conversationId, targetUserId);
        ConversationMember owner = requireMember(conversationId, actorId);
        owner.setRole(ConversationMemberRole.ADMIN); target.setRole(ConversationMemberRole.OWNER);
    }
    @Transactional
    public ConversationMember requireMember(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        if (conversation.getConversationType() != ConversationType.GROUP) throw new IllegalArgumentException("This action is only available for groups");
        ConversationMember member = memberRepository.findByConversationIdAndUserId(conversationId, userId).orElseThrow(() -> new SecurityException("You are not a member of this conversation"));
        if (member.getRole() == ConversationMemberRole.MEMBER && conversation.getCreatedBy().getUserId().equals(userId)) {
            member.setRole(ConversationMemberRole.OWNER);
        }
        return member;
    }
}
