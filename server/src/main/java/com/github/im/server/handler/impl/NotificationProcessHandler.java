package com.github.im.server.handler.impl;

import com.github.im.common.connect.connection.server.ProtoBufProcessHandler;
import com.github.im.common.connect.model.proto.BaseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.netty.Connection;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/2/18
 */
@Component
@Slf4j
public class NotificationProcessHandler implements ProtoBufProcessHandler {

    @Override
    public BaseMessage.BaseMessagePkg.PayloadCase type() {
        return BaseMessage.BaseMessagePkg.PayloadCase.NOTIFICATION;
    }

    @Override
    public void process(Connection con, BaseMessage.BaseMessagePkg message) throws IllegalArgumentException {



    }



}