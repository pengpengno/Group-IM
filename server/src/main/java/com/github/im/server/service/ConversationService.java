package com.github.im.server.service;

import com.github.im.server.mapstruct.ConversationMapper;
import com.github.im.server.model.Conversation;
import com.github.im.server.model.User;
import com.github.im.server.model.enums.ConversationStatus;
import com.github.im.server.model.enums.ConversationType;
import com.github.im.server.repository.ConversationRepository;
import com.github.im.conversation.ConversationDTO;
import com.github.im.dto.user.UserInfo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final GroupMemberService groupMemberService;

    /**
     * 创建新群组
     * @param groupName 群组名称
     * @param description 群组描述
     * @param members 群组成员列表
     * @return 创建后的群组
     */
    @Transactional
    public Conversation createGroup(String groupName, String description, List<UserInfo> members) {
        if (members == null || members.isEmpty()) {
            throw new IllegalArgumentException("Group members cannot be null or empty");
        }

        User owner = new User();  // 假设你已经有 User 实体类
        owner.setUserId(members.get(0).getUserId());

        Conversation group = Conversation.builder()
                .groupName(groupName)
                .description(description)
                .conversationType(ConversationType.GROUP)
                .status(ConversationStatus.ACTIVE)
                .build();

        // 保存群组
        group = conversationRepository.save(group);

        // 添加群成员
        for (UserInfo memberInfo : members) {
            groupMemberService.addMemberToGroup(group.getConversationId(), memberInfo.getUserId());
        }

        return group;
    }

    /**
     * 创建或获取私聊会话
     * @param userId1 第一个用户ID
     * @param userId2 第二个用户ID
     * @return 私聊会话的DTO
     */
    @Transactional
    public ConversationDTO createOrGetPrivateChat(Long userId1, Long userId2) {
        // 查询是否已经存在私聊会话
        Optional<Conversation> existingConversation = conversationRepository.findPrivateChatBetweenUsers(userId1, userId2
                ,ConversationType.PRIVATE_CHAT ,ConversationStatus.ACTIVE);
        if (existingConversation.isPresent()) {
            return convertToDTO(existingConversation.get());
        } else {
            // 创建新的私聊会话
            Conversation newConversation = Conversation.builder()
                    .conversationType(ConversationType.PRIVATE_CHAT)
                    .status(ConversationStatus.ACTIVE)
                    .build();

            // 保存会话
            newConversation = conversationRepository.save(newConversation);

            // 添加成员
            groupMemberService.addMemberToGroup(newConversation.getConversationId(), userId1);
            groupMemberService.addMemberToGroup(newConversation.getConversationId(), userId2);

            return convertToDTO(newConversation);
        }
    }

    private ConversationDTO convertToDTO(Conversation conversation) {
        // 假设 ConversationDTO 已经存在并有合适的构造函数
//        return new ConversationDTO(conversation);
        return ConversationMapper.INSTANCE.toDTO(conversation);
    }

    /**
     * 根据群组ID查询群组信息
     * @param groupId 群组ID
     * @return 群组信息
     */
    public Optional<Conversation> getGroupById(Long groupId) {
        return conversationRepository.findById(groupId);
    }

    /**
     * 根据群组名称查询群组
     * @param groupName 群组名称
     * @return 群组信息
     */
    public Optional<Conversation> getGroupByName(String groupName) {
        return conversationRepository.findByGroupName(groupName);
    }

    /**
     * 获取某个用户加入的所有群组
     * @param userId 用户ID
     * @return 用户的所有群组
     */
    public List<Conversation> getGroupsByUserId(Long userId) {
        return conversationRepository.findByMembers_User_UserId(userId);
    }

    /**
     * 删除群组
     * @param groupId 群组ID
     */
    @Transactional
    public void deleteGroup(Long groupId) {
        Optional<Conversation> group = conversationRepository.findById(groupId);
        group.ifPresent(conversation -> {
            // 删除群组成员
            group.get().getMembers().forEach(member -> groupMemberService.removeMemberFromGroup(groupId, member.getUser().getUserId()));
            // 删除群组
            conversationRepository.delete(conversation);
        });
    }

    /**
     * 获取某个用户正在进行的群组
     * @param userId 用户ID
     * @return 用户正在进行的群组
     */
    public List<Conversation> getActiveConversationsByUserId(Long userId) {
        return conversationRepository.findActiveConversationsByUserId(userId, ConversationStatus.ACTIVE);
    }
}