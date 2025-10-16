package com.github.im.server.service;

import com.github.im.dto.user.FriendRequestDto;
import com.github.im.dto.user.FriendshipDTO;
import com.github.im.server.mapstruct.FriendShipMapper;
import com.github.im.server.model.Friendship;
import com.github.im.server.model.User;
import com.github.im.server.model.enums.Status;
import com.github.im.server.repository.ConversationRepository;
import com.github.im.server.repository.FriendshipRepository;
import com.github.im.server.repository.UserRepository;
import com.github.im.server.service.impl.SendMessageToClientEndPointImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FriendshipService {

    private final UserRepository userRepository;

    private final FriendshipRepository friendshipRepository;

    private final SendMessageToClientEndPointImpl sendMessageToClientEndPoint;

    private final ConversationRepository conversationRepository;

    @Transactional
    public void sendFriendRequest(FriendRequestDto request) {
        Long userId = request.getUserId();
        Long friendId = request.getFriendId();

        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("不能添加自己为好友");
        }

        // 验证好友关系是否存在
        var user = userRepository.findById(userId).orElseThrow();
        var friend = userRepository.findById(friendId).orElseThrow();
        friendshipRepository.findByUserAndFriend(
                user,
                friend
        ).ifPresent(e-> {
            throw new IllegalArgumentException("好友关系已存在");
        });
        userRepository.findByUserIdIn(List.of(userId, friendId))
                .ifPresentOrElse(e-> {
                    if(e.size()==2){
                        Friendship friendship = new Friendship();
                        // 用于处理用户的添加方
                        friendship.setUser(user);
                        friendship.setFriend(friend);
                        // 默认为 pending  等待接受后更新
                        friendship.setStatus(Status.PENDING);
                        friendship.setApplyRemark(request.getApplyRemark());
                        friendshipRepository.save(friendship);
                        sendMessageToClientEndPoint.sendMessage(friendship);
                    }
                }, ()-> {
                    throw new IllegalArgumentException("用户不存在");
                });
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
        // 查询与好友是否存在单聊(只存在两个人的群聊)  存在则返回其 conversationId
//        conversationRepository.findPrivateChatBetweenUsers(userId , )
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
