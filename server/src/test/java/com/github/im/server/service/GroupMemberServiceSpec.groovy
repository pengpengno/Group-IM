package com.github.im.server.service

import com.github.im.server.model.Conversation
import com.github.im.server.model.ConversationMember
import com.github.im.server.model.User
import com.github.im.server.repository.ConversationRepository
import com.github.im.server.repository.GroupMemberRepository
import spock.lang.Specification

class GroupMemberServiceSpec extends Specification {

    def groupMemberRepository = Mock(GroupMemberRepository)
    def conversationRepository = Mock(ConversationRepository)

    def groupMemberService = new GroupMemberService(groupMemberRepository, conversationRepository)

    def "getMembersByConversationId should return list of members"() {
        given:
        def conversationId = 1L
        def members = [
                new ConversationMember(id: 1L, user: new User(userId: 1L)),
                new ConversationMember(id: 2L, user: new User(userId: 2L))
        ]

        groupMemberRepository.findByConversationId(conversationId) >> members

        when:
        def result = groupMemberService.getMembersByConversationId(conversationId)

        then:
        result.size() == 2
        result[0].getId() == 1L
        result[1].getId() == 2L
    }

    def "getGroupsByUserId should return list of group memberships"() {
        given:
        def userId = 1L
        def memberships = [
                new ConversationMember(id: 1L, conversation: new Conversation(conversationId: 1L)),
                new ConversationMember(id: 2L, conversation: new Conversation(conversationId: 2L))
        ]

        groupMemberRepository.findByUserId(userId) >> memberships

        when:
        def result = groupMemberService.getGroupsByUserId(userId)

        then:
        result.size() == 2
        result[0].getId() == 1L
        result[1].getId() == 2L
    }

    def "getMemberByGroupIdAndUserId should return member when exists"() {
        given:
        def conversationId = 1L
        def userId = 1L
        def member = new ConversationMember(id: 1L)

        groupMemberRepository.findByConversationIdAndUserId(conversationId, userId) >> Optional.of(member)

        when:
        def result = groupMemberService.getMemberByGroupIdAndUserId(conversationId, userId)

        then:
        result.isPresent()
        result.get().getId() == 1L
    }

    def "getMemberByGroupIdAndUserId should return empty when not exists"() {
        given:
        def conversationId = 1L
        def userId = 1L

        groupMemberRepository.findByConversationIdAndUserId(conversationId, userId) >> Optional.empty()

        when:
        def result = groupMemberService.getMemberByGroupIdAndUserId(conversationId, userId)

        then:
        !result.isPresent()
    }

    def "addMemberToGroup should save and return new member"() {
        given:
        def groupId = 1L
        def userId = 1L
        def member = new ConversationMember(
                conversation: new Conversation(conversationId: groupId),
                user: new User(userId: userId),
                joinedAt: LocalDateTime.now()
        )

        groupMemberRepository.save(_) >> member

        when:
        def result = groupMemberService.addMemberToGroup(groupId, userId)

        then:
        result.getConversation().getConversationId() == groupId
        result.getUser().getUserId() == userId
        result.getJoinedAt() != null
        1 * groupMemberRepository.save(_)
    }

    def "addMembersToGroup should save all members and return count"() {
        given:
        def conversationId = 1L
        def userIds = [1L, 2L, 3L]
        def conversation = new Conversation(conversationId: conversationId)
        
        conversationRepository.findById(conversationId) >> Optional.of(conversation)

        when:
        def result = groupMemberService.addMembersToGroup(conversationId, userIds)

        then:
        result == 3
        1 * groupMemberRepository.saveAll({ List<ConversationMember> members ->
            members.size() == 3 &&
            members[0].getUser().getUserId() == 1L &&
            members[1].getUser().getUserId() == 2L &&
            members[2].getUser().getUserId() == 3L
        })
    }

    def "addMembersToGroup should throw exception when group not found"() {
        given:
        def conversationId = 1L
        def userIds = [1L, 2L]
        
        conversationRepository.findById(conversationId) >> Optional.empty()

        when:
        groupMemberService.addMembersToGroup(conversationId, userIds)

        then:
        thrown(IllegalArgumentException)
        0 * groupMemberRepository.saveAll(_)
    }

    def "removeMemberFromGroup should delete member when exists"() {
        given:
        def groupId = 1L
        def userId = 1L
        def member = new ConversationMember(id: 1L)

        groupMemberRepository.findByConversationIdAndUserId(groupId, userId) >> Optional.of(member)

        when:
        groupMemberService.removeMemberFromGroup(groupId, userId)

        then:
        1 * groupMemberRepository.delete(member)
    }

    def "removeMemberFromGroup should do nothing when member not exists"() {
        given:
        def groupId = 1L
        def userId = 1L

        groupMemberRepository.findByConversationIdAndUserId(groupId, userId) >> Optional.empty()

        when:
        groupMemberService.removeMemberFromGroup(groupId, userId)

        then:
        0 * groupMemberRepository.delete(_)
    }
}