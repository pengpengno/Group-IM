package com.github.im.server.service;

import com.github.im.conversation.ConversationRes
import com.github.im.enums.ConversationStatus
import com.github.im.enums.ConversationType
import com.github.im.server.mapstruct.ConversationMapper
import com.github.im.server.mapstruct.GroupMemberMapper
import com.github.im.server.model.Conversation
import com.github.im.dto.user.UserInfo
import com.github.im.server.model.ConversationMember
import com.github.im.server.model.User
import com.github.im.server.repository.ConversationRepository
import com.github.im.server.repository.GroupMemberRepository
import com.github.im.server.repository.UserRepository
import jakarta.persistence.EntityManager
import spock.lang.Specification

class ConversationServiceSpec extends Specification {

    private ConversationService conversationService

    private ConversationRepository conversationRepository = Mock()
    private GroupMemberRepository groupMemberRepository = Mock()
    private UserRepository userRepository = Mock()
    private ConversationMapper conversationMapper = Mock()
    private GroupMemberMapper groupMemberMapper = Mock()
    private GroupMemberService groupMemberService
    private final ConversationSequenceService conversationSequenceService = Mock();
    private final EntityManager entityManager = Mock();


    def setup() {
        groupMemberService = Mock(GroupMemberService, constructorArgs: [groupMemberRepository, conversationRepository])

        conversationService = new ConversationService(
                conversationRepository,
                userRepository,
                conversationMapper,
                groupMemberMapper,
                groupMemberService,
                groupMemberRepository,
                conversationSequenceService,
                entityManager
        )
    }

    def "testCreateGroup_Success"() {
        given: "准备测试数据"
        // 测试用的组信息
        String groupName = "Test Group"
        String description = "A test group"

        // 模拟的用户信息
        UserInfo userInfo1 = new UserInfo(userId: 1L)
        UserInfo userInfo2 = new UserInfo(userId: 2L)

        def user = new User(userId: 1L)
        def user2 = new User(userId: 2L)
        List<UserInfo> members = [userInfo1, userInfo2]

        // 创建 ConversationMember 对象
        def member1 = new ConversationMember(id: 1L, user: user)
        def member2 = new ConversationMember(id: 2L, user: user2)
        List<ConversationMember> memberList = [member1, member2]

        // 构建保存的对话
        Conversation savedConversation = Conversation.builder()
                .conversationId(1L)
                .groupName(groupName)
                .description(description)
                .conversationType(ConversationType.GROUP)
                .members(memberList)
                .status(ConversationStatus.ACTIVE)
                .build()

        // 模拟的返回响应
        ConversationRes mockResponse = new ConversationRes(
                conversationId: 1L,
                groupName: groupName,
                description: description,
                conversationType: ConversationType.GROUP,
                status: ConversationStatus.ACTIVE,
                members: members
        )

        // 模拟 repository 和 mapper 的行为
        conversationRepository.saveAndFlush(_ as Conversation) >> savedConversation
        entityManager.getReference(Conversation.class, 1L) >> savedConversation
        userRepository.getReferenceById(1L) >> user
        userRepository.getReferenceById(2L) >> user2
        conversationMapper.toDTO(savedConversation) >> mockResponse
        groupMemberRepository.saveAll(_ as List) >> memberList

        // Mock groupMemberService.addMemberToGroup 的调用
        groupMemberService.addMemberToGroup(_ as Long, _ as Long) >> {}

        when: "执行测试"
        // 调用实际的创建群组服务方法
        ConversationRes result = conversationService.createGroup(userInfo1.getUserId(), groupName, description, members)

        then: "验证结果"
        // 验证结果是否与预期一致
        result == mockResponse
//
//        // 验证保存对话的调用
        1 * conversationRepository.saveAndFlush({ Conversation conv ->
                    conv.groupName == groupName &&
                    conv.description == description &&
                    conv.conversationType == ConversationType.GROUP &&
                    conv.status == ConversationStatus.ACTIVE
        }) >> savedConversation
//
//        // 验证添加成员到群组的方法调用
        1 * groupMemberRepository.saveAll(_ as List) >> {}
//
//        // 验证映射器转换为 DTO
        1 * conversationMapper.toDTO(savedConversation) >> mockResponse
    }


    def "testCreateGroup_WithEmptyMembersList_ThrowsException"() {
        given: "准备测试数据"
        String groupName = "Test Group"
        String description = "A test group"
        Long createUserId = 1L
        List<UserInfo> emptyMembers = []

        when: "执行测试"
        conversationService.createGroup(createUserId, groupName, description, emptyMembers)

        then: "验证异常"
        IllegalArgumentException exception = thrown()
        exception.message.contains("Group members cannot be null or empty")
        
        0 * conversationRepository.save(_ as Conversation)
        0 * groupMemberService.addMemberToGroup(_ as Long, _ as Long)
        0 * conversationMapper.toDTO(_ as Conversation)
    }

    def "testCreateGroup_WithNullMembers_ThrowsException"() {
        given: "准备测试数据"
        String groupName = "Test Group"
        String description = "A test group"
        Long createUserId = 1L
        List<UserInfo> nullMembers = null

        when: "执行测试"
        conversationService.createGroup(createUserId, groupName, description, nullMembers)

        then: "验证异常"
        IllegalArgumentException exception = thrown()
        exception.message.contains("Group members cannot be null or empty")
        
        0 * conversationRepository.save(_ as Conversation)
        0 * groupMemberService.addMemberToGroup(_ as Long, _ as Long)
        0 * conversationMapper.toDTO(_ as Conversation)
    }
}
