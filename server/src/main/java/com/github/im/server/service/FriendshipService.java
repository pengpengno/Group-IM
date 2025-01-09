package com.github.im.server.service;

import com.github.im.dto.user.FriendRequestDto;
import com.github.im.dto.user.FriendshipDTO;
import com.github.im.server.mapstruct.FriendShipMapper;
import com.github.im.server.model.Friendship;
import com.github.im.server.model.User;
import com.github.im.server.model.enums.Status;
import com.github.im.server.repository.FriendshipRepository;
import com.github.im.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.List;

@Service
public class FriendshipService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Transactional
    public void sendFriendRequest(FriendRequestDto request) {
        User user = userRepository.findById(request.getUserId()).orElseThrow();
        User friend = userRepository.findById(request.getFriendId()).orElseThrow();
        Friendship friendship = new Friendship();
        friendship.setUser(user);
        friendship.setFriend(friend);
        friendshipRepository.save(friendship);
    }

    @Transactional
    public void acceptFriendRequest(FriendRequestDto request) {
        Friendship friendship = friendshipRepository.findByUserAndFriend(
                userRepository.findById(request.getFriendId()).orElseThrow(()->
                        new IllegalArgumentException(MessageFormat.format("No user id {0}",request.getFriendId()))),
                userRepository.findById(request.getUserId()).orElseThrow()
        ).orElseThrow();

        friendshipRepository.save(friendship);
    }


    /**
     * 获取用户的好友列表
     */
    public List<FriendshipDTO> getFriends(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        var byUserAndStatus = friendshipRepository.findByUserAndStatus(user, Status.ACTIVE);
        return FriendShipMapper.INSTANCE.friendshipsToFriendshipDTOs(byUserAndStatus);
    }

    /**
     * 删除好友关系
     */
    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        User user = userRepository.findById(userId).orElseThrow();
        User friend = userRepository.findById(friendId).orElseThrow();

        // 删除好友关系
        Friendship friendship = friendshipRepository.findByUserAndFriend(user, friend)
                .orElseThrow(() -> new IllegalStateException("好友关系不存在"));
        friendshipRepository.delete(friendship);
    }

}
