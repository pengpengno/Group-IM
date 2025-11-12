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
import lombok.val;
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
    private final FriendShipMapper friendShipMapper;

    @Transactional
    public FriendshipDTO sendFriendRequest(FriendRequestDto request) {
        final Long userId = request.getUserId();
        final Long friendId = request.getFriendId();

        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("不能添加自己为好友");
        }

        // 验证好友关系是否存在
        var user = userRepository.findById(userId).orElseThrow();
        var friend = userRepository.findById(friendId).orElseThrow();
        return friendshipRepository.findByUserAndFriend(
                user,
                friend
        ).map(friendShipMapper::friendshipToFriendshipDTO).orElseGet(()-> {
            Friendship friendship = new Friendship();
            // 用于处理用户的添加方
            friendship.setUser(user);
            friendship.setFriend(friend);
            // 默认为 pending  等待接受后更新
            friendship.setStatus(Status.PENDING);
            friendship.setApplyRemark(request.getApplyRemark());
            sendMessageToClientEndPoint.sendMessage(friendship);
            Friendship save = friendshipRepository.save(friendship);
            return friendShipMapper.friendshipToFriendshipDTO(save);
        });


    }

    @Transactional
    public void acceptFriendRequest(FriendRequestDto request) {
        Friendship friendship = friendshipRepository.findByUserAndFriend(
                userRepository.findById(request.getFriendId()).orElseThrow(()->
                        new IllegalArgumentException(MessageFormat.format("No user id {0}",request.getFriendId()))),
                userRepository.findById(request.getUserId()).orElseThrow()
        ).orElseThrow();
        friendship.setStatus(Status.ACTIVE);
        friendshipRepository.save(friendship);
    }

   public void updateFriendRequestStatus(Long userId , Long friendId , Status  status ){

       List<Friendship> friendships = friendshipRepository.findBetweenUsers(userId, friendId).orElseThrow(
               ()-> new IllegalArgumentException("好友关系不存在")
       );

       switch ( status){
           case PENDING:
               var findByUser_UserIdAndFriend_UserId = friendshipRepository.findByUser_UserIdAndFriend_UserId(userId, friendId);
               if(findByUser_UserIdAndFriend_UserId.isEmpty()){
                   throw new IllegalArgumentException("好友关系不存在");
               }
               break;
           case ACTIVE:
           case BLOCKED:
           case REJECT:
               // 批量更新状态
               friendships.forEach(e->e.setStatus(status));
               friendshipRepository.saveAll(friendships);
               break;
       }
   }


    /**
     * 获取用户的好友列表
     */
    public List<FriendshipDTO> getFriends(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        var byUserAndStatus = friendshipRepository.findByUserAndStatus(user, Status.ACTIVE);
        // 查询与好友是否存在单聊(只存在两个人的群聊)  存在则返回其 conversationId
//        conversationRepository.findPrivateChatBetweenUsers(userId , )
        return friendShipMapper.friendshipsToFriendshipDTOs(byUserAndStatus);
    }

//    /**
//     * 查询用户发起的好友请求列表
//     * @param userId 用户ID
//     * @return 好友请求列表
//     */
//    public List<FriendshipDTO> getSentFriendRequests(Long userId) {
//        // 使用JPA查询方法直接查询用户发起的、状态为PENDING的好友请求
//        var pendingRequests = friendshipRepository.findByUser_UserIdAndStatus(userId, Status.PENDING);
//        return friendShipMapper.friendshipsToFriendshipDTOs(pendingRequests);
//    }

    /**
     * 查询用户发起的好友请求列表
     * @param userId 用户ID
     * @return 好友请求列表
     */
    public List<FriendshipDTO> getFriendRequests(Long userId) {
        // 查询用户发起的、状态为PENDING的好友请求
        val allFriendShips = friendshipRepository.findByUserIdOrFriendIdPENDING(userId);
//        var pendingRequests = friendshipRepository.findByUserAndStatus(user, Status.PENDING);
        return friendShipMapper.friendshipsToFriendshipDTOs(allFriendShips);
    }

    /**
     * 同步好友请求
     * 根据客户端传入的最大关系ID，获取之后的新关系数据
     * @param userId 用户ID
     * @param maxId 客户端当前最大的关系ID
     * @return 新的好友关系列表
     */
    public List<FriendshipDTO> syncFriendRequests(Long userId, Long maxId) {
        List<Friendship> friendships = friendshipRepository.findFriendshipsAfterId(userId, maxId);
        return friendShipMapper.friendshipsToFriendshipDTOs(friendships);
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
