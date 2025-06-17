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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;


/**
 * Description:
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


    public void sendMessage(ChatMsgVo request) {

    }


    /***
     * 向客户端发送好友请求消息
     * @param friendship
     */
    public void sendMessage(final Friendship friendship) {

        var friend = friendship.getFriend(); // 被请求人
        var user = friendship.getUser();  // 申请人
        if(friend == null){
            return;
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


        var friendAccount = friend.getAccount();

        BindAttr<String> bindAttr = BindAttr.getBindAttrForPush(friendAccount);


        BaseMessage.BaseMessagePkg baseMessagePkg =
                BaseMessage.BaseMessagePkg.newBuilder()
                .setNotification(notificationInfo)
                .build();
        // 在线的化就发送消息 不在线 就屏蔽
        ReactiveConnectionManager.addBaseMessage(bindAttr, baseMessagePkg);

    }



}