package com.github.im.server.service;

import com.github.im.server.model.Conversation;
import com.github.im.server.model.User;
import com.github.im.server.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
     * @param ownerId 群主用户ID
     * @return 创建后的群组
     */
    @Transactional
    public Conversation createGroup(String groupName, String description, Long ownerId) {
        User owner = new User();  // 假设你已经有 User 实体类
        owner.setUserId(ownerId);

        Conversation group = Conversation.builder()
                .groupName(groupName)
                .description(description)
                .build();

        // 保存群组
        group = conversationRepository.save(group);

        // 添加群主作为群成员
        groupMemberService.addMemberToGroup(group.getConversationId(), ownerId);

        return group;
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
}
