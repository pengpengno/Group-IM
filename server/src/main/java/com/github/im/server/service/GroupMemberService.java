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



        /**
         * 获取某个群组的所有成员
         * @param ConversationId 群组ID
         * @return 群组成员列表
         */
        public List<ConversationMember> getMembersByConversationId(Long ConversationId) {
            return groupMemberRepository.findByConversationId(ConversationId);
        }

        /**
         * 获取某个用户的所有群组
         * @param userId 用户ID
         * @return 用户的群组成员列表
         */
        public List<ConversationMember> getGroupsByUserId(Long userId) {
            return groupMemberRepository.findByUserId(userId);
        }

        /**
         * 根据群组ID和用户ID查找某个群组中的某个成员
         * @param ConversationId 群组ID
         * @param userId 用户ID
         * @return 成员信息
         */
        public Optional<ConversationMember> getMemberByGroupIdAndUserId(Long ConversationId, Long userId) {
            return groupMemberRepository.findByConversationIdAndUserId(ConversationId, userId);
        }


        /**
         * 添加新成员到群组
         * @param groupId 群组ID
         * @param userId 用户ID
         * @return 添加后的群组成员
         */
        public ConversationMember addMemberToGroup(@NotNull(message = "Group ID cannot be null") Long groupId,
                                                   @NotNull(message = "add memberUserid not be null" )Long userId) {

            var groupMember = ConversationMember.builder()
                    .conversation(Conversation.builder().conversationId(groupId).build())
                    .user(User.builder().userId(userId).build())
                    .joinedAt(LocalDateTime.now())
                    .build();


            return groupMemberRepository.saveAndFlush(groupMember);
        }

        /**
         * 批量添加多个用户到同一个群组
         * @param conversationId 群组ID
         * @param userIds 用户ID列表
         * @return 添加的用户数量
         */
        @Transactional
        public int addMembersToGroup(@NotNull Long conversationId,@NotEmpty List<Long> userIds) {
//            Conversation group = conversationService.getGroupById(groupId)
            Conversation group = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new IllegalArgumentException("Group not found"));

            // 批量保存用户成员
            List<ConversationMember> conversationMembers = userIds.stream()
                    .map(userId -> ConversationMember.builder()
                            .conversation(group)
                            .user(User.builder().userId(userId).build()).build())
                    .collect(Collectors.toList());

            groupMemberRepository.saveAll(conversationMembers); // 批量保存成员

            return conversationMembers.size();  // 返回添加的成员数量
        }

        /**
         * 从群组中移除成员
         * @param groupId 群组ID
         * @param userId 用户ID
         * @return 删除的成员
         */
        @Transactional
        public void removeMemberFromGroup(Long groupId, Long userId) {
            Optional<ConversationMember> groupMemberOptional = groupMemberRepository.findByConversationIdAndUserId(groupId, userId);
            groupMemberOptional.ifPresent(groupMember -> groupMemberRepository.delete(groupMember));
        }

        /**
         * 更新成员的角色
         * @param groupId 群组ID
         * @param userId 用户ID
         * @param newRole 新的角色
         * @return 更新后的成员
         */
    //    @Transactional
    //    public GroupMember updateMemberRole(Long groupId, Long userId, Role newRole) {
    //        Optional<GroupMember> groupMemberOptional = groupMemberRepository.findByGroupIdAndUserId(groupId, userId);
    //        if (groupMemberOptional.isPresent()) {
    //            GroupMember groupMember = groupMemberOptional.get();
    //            groupMember.setRole(newRole);
    //            return groupMemberRepository.save(groupMember);
    //        }
    //        return null;
    //    }
    }
