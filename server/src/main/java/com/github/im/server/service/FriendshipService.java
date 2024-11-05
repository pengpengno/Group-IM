package com.github.im.server.service;

import com.github.im.dto.user.FriendRequestDto;
import com.github.im.server.model.Friendship;
import com.github.im.server.model.User;
import com.github.im.server.repository.FriendshipRepository;
import com.github.im.server.repository.UserRepository;
import io.lettuce.core.RedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        friendship.setStatus("requested");
        friendshipRepository.save(friendship);
    }

    @Transactional
    public void acceptFriendRequest(FriendRequestDto request) {
        Friendship friendship = friendshipRepository.findByUserAndFriend(
                userRepository.findById(request.getFriendId()).orElseThrow(),
                userRepository.findById(request.getUserId()).orElseThrow()
        ).orElseThrow();
        friendship.setStatus("accepted");
        friendshipRepository.save(friendship);
    }

    @Transactional
    public void deleteFriend(Long friendId) {
        // Implement logic to delete friendship
    }
}
