package com.github.im.server.service.impl;

import com.github.im.common.connect.connection.ReactiveConnectionManager;
import com.github.im.common.connect.connection.server.BindAttr;
import com.github.im.common.connect.enums.PlatformType;
import com.github.im.common.connect.model.proto.Account;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.common.connect.model.proto.Notification;
import com.github.im.common.model.account.ChatMsgVo;
import com.github.im.dto.user.FriendRequestDto;
import com.github.im.server.service.SendMessageToClientEndPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


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


    public void sendMessage(FriendRequestDto request) {


        var friendId = request.getFriendId();

        Notification.NotificationInfo notificationInfo = Notification.NotificationInfo.newBuilder()
                .setFriendRequest(Notification.NotificationInfo.
                                FriendRequest.newBuilder()
                        .setFromUserId(request.getUserId().intValue())
                        .setToUserId(friendId.intValue())
                        .setFromUserName(request.getUserName())
                        .setToUserName(request.getFriendName())
                        .setRemark(request.getRemark())
                        .build())
                .build();


        var friendAccount = request.getFriendAccount();

        BindAttr<String> bindAttr = BindAttr.getBindAttr(friendAccount);


        BaseMessage.BaseMessagePkg baseMessagePkg =
                BaseMessage.BaseMessagePkg.newBuilder()
                .setNotification(notificationInfo)
                .build();

        ReactiveConnectionManager.addBaseMessage(bindAttr, baseMessagePkg);



    }



}