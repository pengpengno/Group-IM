package com.github.im.server.service;

import com.github.im.dto.user.FriendRequestDto;
import com.github.im.dto.user.FriendshipDTO;
import com.github.im.server.mapstruct.FriendShipMapper;
import com.github.im.server.model.Friendship;
import com.github.im.server.model.User;
import com.github.im.server.model.enums.Status;
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

    @Transactional
    public void sendFriendRequest(FriendRequestDto request) {
        Long userId = request.getUserId();
        Long friendId = request.getFriendId();

        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("不能添加自己为好友");
        }
        userRepository.findByUserIdIn(List.of(userId, friendId))
                .ifPresentOrElse(e-> {

                    // TODO 目前设置单向 联系人即可
                    if(e.size()==2){
                        Friendship friendship = new Friendship();
                        var firstUser = e.get(0);
                        var lastUser = e.get(1);
                        if(firstUser.getUserId().equals(userId)){
                            // 用于处理用户的添加方
                            friendship.setUser(firstUser);
                            friendship.setFriend(lastUser);
                        }else{
                            friendship.setUser(lastUser);
                            friendship.setFriend(firstUser);
                        }
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
