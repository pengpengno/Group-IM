package com.github.im.server.service;

import com.github.im.conversation.ConversationRes;
import com.github.im.server.mapstruct.GroupMemberMapper;
import com.github.im.server.model.Conversation;
import com.github.im.server.model.ConversationMember;
import com.github.im.server.model.User;
import com.github.im.enums.ConversationStatus;
import com.github.im.enums.ConversationType;
import com.github.im.server.repository.ConversationRepository;
import com.github.im.server.mapstruct.ConversationMapper;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.repository.GroupMemberRepository;
import com.github.im.server.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository ;
    private final ConversationMapper conversationsMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final GroupMemberService groupMemberService;
    private final GroupMemberRepository groupMemberRepository;
    private final ConversationSequenceService conversationSequenceService;
    private final EntityManager entityManager;

    /**
     * 创建新群组
     * @param groupName 群组名称
     * @param description 群组描述
     * @param members 群组成员列表
     * @return 创建后的群组
     */
    public ConversationRes createGroup(String groupName, String description, List<UserInfo> members) {
        if (members == null || members.isEmpty()) {
            throw new IllegalArgumentException("Group members cannot be null or empty");
        }

        User owner = new User();
        owner.setUserId(members.getFirst().getUserId());

        Conversation group = Conversation.builder()
                .groupName(groupName)
                .description(description)
                .conversationType(ConversationType.GROUP)
                .status(ConversationStatus.ACTIVE)
                .createdBy(owner)
                .build();
        // 保存群组
        final var saveGroup = conversationRepository.saveAndFlush(group);
        Conversation reference = entityManager.getReference(Conversation.class, saveGroup.getConversationId());
        var groupMembers =  members.stream().map(member -> {
            User us = userRepository.getReferenceById(member.getUserId());
            return ConversationMember.builder()
                    .conversation(reference)
                    .user(us)
                    .joinedAt(LocalDateTime.now())
                    .build();
        }).toList();
        List<ConversationMember> conversationMembers = groupMemberRepository.saveAll(groupMembers);

        return conversationsMapper.toDTO(saveGroup);
    }

    public Long  maxIndex(Long conversationId){
        return conversationSequenceService.getMaxSequence(conversationId);
    }

    public ConversationRes getConversationById(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId).orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        return conversationsMapper.toDTO(conversation);
    }
    /**
     * 创建或获取私聊会话
     * @param userId1 第一个用户ID  group creator
     * @param userId2 第二个用户ID
     * @return 私聊会话的DTO
     */
    public ConversationRes createOrGetPrivateChat(Long userId1, Long userId2) {
        // 查询是否已经存在私聊会话
        Optional<Conversation> existingConversation = conversationRepository.findPrivateChatBetweenUsers(userId1, userId2        )
                ;
        if (existingConversation.isPresent()) {
            var conversation = existingConversation.get();
            var members = conversation.getMembers();
            return conversationsMapper.toDTO(conversation);
        } else {
            // 创建新的私聊会话
            Conversation newConversation = Conversation.builder()
                    .conversationType(ConversationType.PRIVATE_CHAT)
                    .status(ConversationStatus.ACTIVE)
                    .createdBy(userRepository.findById(userId1).orElseThrow(()-> new UsernameNotFoundException("User not found")))
                    .build();

            // 保存会话
//            var members = newConversation.getMembers();
            Conversation savedConversation = conversationRepository.save(newConversation);
            newConversation = conversationRepository.findById(savedConversation.getConversationId()).get();


            ConversationMember conversationMember1 = ConversationMember.builder()
                    .conversation(savedConversation)
                    .user( userRepository.getReferenceById(userId1))
                    .build();
            ConversationMember conversationMember2 = ConversationMember.builder()
                    .conversation(savedConversation)
                    .user( userRepository.getReferenceById(userId2))
                    .build();
            groupMemberRepository.saveAll(List.of(conversationMember1, conversationMember2));
            // 添加成员
//            groupMemberService.addMemberToGroup(newConversation.getConversationId(), userId1);
//            groupMemberService.addMemberToGroup(newConversation.getConversationId(), userId2);

            /**
             * 这里私聊不会 保存  群组的名称 , 在 客户端 互相使用 对方的名称来展示
             */
            var dto = conversationsMapper.toDTO(newConversation);
//            dto.setGroupName(newConversation.getGroupName());
            return dto;
        }
    }

    /**
     * 根据群组ID查询群组信息
     * @param groupId 群组ID
     * @return 群组信息
     */
    public Optional<ConversationRes> getGroupById(Long groupId) {
        return conversationRepository.findById(groupId).map(conversationsMapper::toDTO);
    }

    /**
     * 根据群组名称查询群组
     * @param groupName 群组名称
     * @return 群组信息
     */
    public Optional<ConversationRes> getGroupByName(String groupName) {
        return conversationRepository.findByGroupName(groupName).map(conversationsMapper::toDTO);
    }

    /**
     * 获取某个用户加入的所有群组
     * @param userId 用户ID
     * @return 用户的所有群组
     */
    public List<ConversationRes> getGroupsByUserId(Long userId) {
        return conversationRepository.findByMembers_User_UserId(userId).stream()
                .map(conversationsMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 删除群组
     * @param groupId 群组ID
     */
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
    public List<ConversationRes> getActiveConversationsByUserId(Long userId) {
//        return conversationRepository.findActiveConversationsByUserId(userId, ConversationStatus.ACTIVE).stream()
        return conversationRepository.findActiveConversationsByUserId(userId).stream()
                .map(conversationsMapper::toDTO)
                .collect(Collectors.toList());
    }



    /**
     * 根据群组ID获取群组成员信息
     *
     * @param groupId 群组ID，用于标识特定的群组
     * @return 群组成员的 UserInfo 对象列表如果群组不存在或成员为空，则返回空列表
     */
    public List<UserInfo> getMembersByGroupId(Long groupId) {
        // 尝试通过群组ID查找群组信息
        Optional<Conversation> group = conversationRepository.findById(groupId);

        // 检查是否找到了对应的群组
        if (group.isPresent()) {
            // 如果群组存在，则将群组成员转换为 UserInfo 对象列表并返回
            var conversation = group.get();
            var members = conversation.getMembers();
            return groupMemberMapper.toUserInfoList(members);
        }
        // 如果没有找到群组，则返回空列表
        return Collections.emptyList();
    }
}