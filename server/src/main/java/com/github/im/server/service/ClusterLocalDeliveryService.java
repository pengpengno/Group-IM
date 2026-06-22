package com.github.im.server.service;

import com.github.im.common.connect.connection.ReactiveConnectionManager;
import com.github.im.common.connect.connection.server.BindAttr;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.server.handler.SignalWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ClusterLocalDeliveryService {

    public void deliverBaseMessage(Long toUserId, BaseMessage.BaseMessagePkg payload) {
        try {
            BindAttr bindAttr = BindAttr.getBindAttrForPush(toUserId.toString());
            ReactiveConnectionManager.addBaseMessage(bindAttr, payload);
        } catch (Exception e) {
            log.error("Failed to deliver message locally to user {}: {}", toUserId, e.getMessage(), e);
            throw new IllegalStateException("Failed to deliver local IM message", e);
        }
    }

    public void deliverSignalText(Long toUserId, String payload) {
        SignalWebSocketHandler signalHandler = SignalWebSocketHandler.getInstance();
        if (signalHandler == null) {
            throw new IllegalStateException("SignalWebSocketHandler is not ready");
        }

        boolean delivered = signalHandler.sendClusterSignal(toUserId.toString(), payload);
        if (!delivered) {
            log.warn("Signal payload dropped because no local WebSocket session was found for user {}", toUserId);
        }
    }
}
