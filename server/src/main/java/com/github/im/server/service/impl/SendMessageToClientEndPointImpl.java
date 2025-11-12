package com.github.im.server.service.impl;

import com.github.im.common.connect.connection.ReactiveConnectionManager;
import com.github.im.common.connect.connection.server.BindAttr;
import com.github.im.common.connect.enums.PlatformType;
import com.github.im.common.connect.model.proto.Account;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.common.connect.model.proto.Notification;
import com.github.im.common.model.account.ChatMsgVo;
import com.github.im.dto.user.FriendRequestDto;
import com.github.im.server.model.Friendship;
import com.github.im.server.model.User;
import com.github.im.server.service.SendMessageToClientEndPoint;
import com.github.im.server.service.dto.ToClientData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;


/**
 * Description: 主动发送消息至客户端
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/2/25
 */
@Slf4j
@Service
public class SendMessageToClientEndPointImpl implements SendMessageToClientEndPoint {


    @Override
    public void send() {




    }




    /***
     * 向客户端发送好友请求消息
     * @param friendship 好友关系
     */
    public void sendMessage(final Friendship friendship) {

        FriendRequest friendRequest = new FriendRequest(friendship);
        Optional<BaseMessage.BaseMessagePkg> pkgOpt = friendRequest.toPkg();

        
        pkgOpt.ifPresent(pkg-> {

            final BindAttr<String> bindAttr = friendRequest.getBindAttr();

            // 在线的化就发送消息 不在线 就屏蔽
            ReactiveConnectionManager.addBaseMessage(bindAttr, pkg);
        });


    }

    public static class FriendRequest implements ToClientData{

        private final Friendship friendship;
        public FriendRequest(Friendship friendship){
            this.friendship = friendship;
        }

        @Override
        public Optional<BaseMessage.BaseMessagePkg> toPkg() {

            var friend = friendship.getFriend(); // 被请求人
            var user = friendship.getUser();  // 申请人
            if(friend == null){
                return Optional.empty();
            }
            var friendId = friend.getUserId();

            Notification.NotificationInfo notificationInfo = Notification.NotificationInfo.newBuilder()
                    .setFriendRequest(Notification.NotificationInfo.
                            FriendRequest.newBuilder()
                            .setFromUserId(user.getUserId().intValue())
                            .setToUserId(friendId.intValue())
                            .setFromUserName(user.getUsername())
                            .setToUserName(friend.getUsername())
                            .setRemark(Optional.ofNullable(friendship.getRemark()).orElse(""))
                            .build())
                    .build();



            BaseMessage.BaseMessagePkg baseMessagePkg =
                    BaseMessage.BaseMessagePkg.newBuilder()
                            .setNotification(notificationInfo)
                            .build();
            return Optional.of(baseMessagePkg);
        }

        @Override
        public BindAttr<String> getBindAttr() {
            User friend = friendship.getFriend();
            var friendAccount = friend.getAccount();

            BindAttr<String> bindAttr = BindAttr.getBindAttrForPush(friendAccount);

            return bindAttr;
        }
    }





}