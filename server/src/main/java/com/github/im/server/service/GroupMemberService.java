package com.github.im.server.service;

import com.github.im.server.model.Conversation;
import com.github.im.server.model.ConversationMember;
import com.github.im.server.model.User;
import com.github.im.server.repository.ConversationRepository;
import com.github.im.server.repository.GroupMemberRepository;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Validated
public class GroupMemberService {

    private final GroupMemberRepository groupMemberRepository;
    private final ConversationRepository conversationRepository;

    public List<ConversationMember> getMembersByConversationId(Long ConversationId) {
        return groupMemberRepository.findByConversationId(ConversationId);
    }

    public List<ConversationMember> getGroupsByUserId(Long userId) {
        return groupMemberRepository.findByUserId(userId);
    }

    public Optional<ConversationMember> getMemberByGroupIdAndUserId(Long ConversationId, Long userId) {
        return groupMemberRepository.findByConversationIdAndUserId(ConversationId, userId);
    }

    public ConversationMember addMemberToGroup(@NotNull(message = "Group ID cannot be null") Long groupId,
                                               @NotNull(message = "add memberUserid not be null") Long userId) {
        var groupMember = ConversationMember.builder()
                .conversation(Conversation.builder().conversationId(groupId).build())
                .user(User.builder().userId(userId).build())
                .joinedAt(LocalDateTime.now())
                .build();

        return groupMemberRepository.saveAndFlush(groupMember);
    }

    @Transactional
    public int addMembersToGroup(@NotNull Long conversationId, @NotEmpty List<Long> userIds) {
        Conversation group = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        var existingMemberIds = groupMemberRepository.findByConversationId(conversationId).stream()
                .map(member -> member.getUser().getUserId())
                .collect(Collectors.toSet());

        List<ConversationMember> conversationMembers = userIds.stream()
                .filter(userId -> userId != null && !existingMemberIds.contains(userId))
                .map(userId -> ConversationMember.builder()
                        .conversation(group)
                        .user(User.builder().userId(userId).build())
                        .joinedAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());

        if (conversationMembers.isEmpty()) {
            return 0;
        }

        groupMemberRepository.saveAll(conversationMembers);
        return conversationMembers.size();
    }

    @Transactional
    public void removeMemberFromGroup(Long groupId, Long userId) {
        Optional<ConversationMember> groupMemberOptional = groupMemberRepository.findByConversationIdAndUserId(groupId, userId);
        groupMemberOptional.ifPresent(groupMemberRepository::delete);
    }
}
