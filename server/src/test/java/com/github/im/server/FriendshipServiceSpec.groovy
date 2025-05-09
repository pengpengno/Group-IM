package com.github.im.server

import com.github.im.common.model.account.ChatMsgVo;
import com.github.im.dto.user.FriendRequestDto
import com.github.im.dto.user.FriendshipDTO
import com.github.im.server.model.Friendship
import com.github.im.server.model.User
import com.github.im.server.repository.FriendshipRepository
import com.github.im.server.repository.UserRepository
import com.github.im.server.service.FriendshipService;
import com.github.im.server.service.impl.SendMessageToClientEndPointImpl
import spock.lang.Specification

class FriendshipServiceSpec extends Specification {

    def userRepository = Mock(UserRepository)
    def friendshipRepository = Mock(FriendshipRepository)
    def endpoint = Mock(SendMessageToClientEndPointImpl)

    def friendshipService = new FriendshipService(userRepository, friendshipRepository, endpoint)

    def "should send friend request and save friendship"() {
        given:
        def request = new FriendRequestDto(userId: 1L, friendId: 2L)
        def user = new User(userId: 1L)
        def friend = new User(userId: 2L)

        def userList = [user, friend]

        userRepository.findById(1L) >> Optional.of(user)
        userRepository.findById(2L) >> Optional.of(friend)

        friendshipRepository.findByUserAndFriend(_ as User, _ as User) >> Optional.empty()

        userRepository.findByUserIdIn(_ as List) >> Optional.of(userList)

        when:
        friendshipService.sendFriendRequest(request)

        then:
        1 * friendshipRepository.save(_)
        1 * endpoint.sendMessage(_)
    }


    def "should throw exception when user not found"() {
        given:
        def request = new FriendRequestDto(userId: 1L, friendId: 2L)
        userRepository.findById(1L) >> Optional.empty()

        when:
        friendshipService.sendFriendRequest(request)

        then:
        thrown(NoSuchElementException)
        0 * friendshipRepository.save(_)
        0 * endpoint.sendMessage(_)
    }


    def "sendFriendRequest should save friendship and notify"() {
        given:
        def request = new FriendRequestDto(userId: 1L, friendId: 2L)
        def user = new User(userId: 1L)
        def friend = new User(userId: 2L)

        userRepository.findById(1L) >> Optional.of(user)
        userRepository.findById(2L) >> Optional.of(friend)

        when:
        friendshipService.sendFriendRequest(request)

        then:
        1 * friendshipRepository.save(_ as Friendship)
        1 * endpoint.sendMessage(request)
    }

    def "acceptFriendRequest should save existing friendship"() {
        given:
        def request = new FriendRequestDto(userId: 1L, friendId: 2L)
        def user = new User(userId: 1L)
        def friend = new User(userId: 2L)
        def friendship = new Friendship(user: friend, friend: user)

        userRepository.findById(1L) >> Optional.of(user)
        userRepository.findById(2L) >> Optional.of(friend)
        friendshipRepository.findByUserAndFriend(friend, user) >> Optional.of(friendship)

        when:
        friendshipService.acceptFriendRequest(request)

        then:
        1 * friendshipRepository.save(friendship)
    }

    def "getFriends should return list of DTOs"() {
        given:
        def user = new User(userId: 1L)
        def friendship = new Friendship(user: user, friend: new User(userId: 2L), status: Status.ACTIVE)

        userRepository.findById(1L) >> Optional.of(user)
        friendshipRepository.findByUserAndStatus(user, Status.ACTIVE) >> List.of(friendship)

        and:
        // mock mapper
        FriendShipMapper.INSTANCE = Mock(FriendShipMapper) {
            friendshipsToFriendshipDTOs(_) >> [new FriendshipDTO(friendId: 2L)]
        }

        when:
        def result = friendshipService.getFriends(1L)

        then:
        result.size() == 1
        result[0].friendId == 2L
    }

    def "deleteFriend should remove friendship"() {
        given:
        def user = new User(userId: 1L)
        def friend = new User(userId: 2L)
        def friendship = new Friendship(user: user, friend: friend)

        userRepository.findById(1L) >> Optional.of(user)
        userRepository.findById(2L) >> Optional.of(friend)
        friendshipRepository.findByUserAndFriend(user, friend) >> Optional.of(friendship)

        when:
        friendshipService.deleteFriend(1L, 2L)

        then:
        1 * friendshipRepository.delete(friendship)
    }

    def "deleteFriend should throw if friendship not exists"() {
        given:
        def user = new User(userId: 1L)
        def friend = new User(userId: 2L)
        userRepository.findById(1L) >> Optional.of(user)
        userRepository.findById(2L) >> Optional.of(friend)

        //  好友关系不存在的情况
        friendshipRepository.findByUserAndFriend(user, friend) >> Optional.empty()

        when:
        friendshipService.deleteFriend(1L, 2L)

        then:
        def ex = thrown(IllegalStateException)
        ex.message == '好友关系不存在'
    }

    def "sendFriendRequest should throw if user not found"() {
        given:
        def request = new FriendRequestDto(userId: 1L, friendId: 2L)
        userRepository.findById(1L) >> Optional.empty()

        when:
        friendshipService.sendFriendRequest(request)

        then:
        thrown(NoSuchElementException)
    }

}
