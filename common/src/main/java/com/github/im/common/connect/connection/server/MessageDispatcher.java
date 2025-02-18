package com.github.im.common.connect.connection.server;

import cn.hutool.core.collection.CollectionUtil;
import com.github.im.common.connect.connection.ConnectionConstants;
import com.github.im.common.connect.connection.ReactiveConnectionManager;
import com.github.im.common.connect.model.proto.Account;
import com.github.im.common.connect.model.proto.BaseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import reactor.netty.Connection;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * message Dispatcher
 * 这里使用 spring 框架来实现 {@link ProtoBufProcessHandler }子类得注入会方便些
 * 实际使用 还是通过 {@code  MessageDispatcher.getInstance()} 来使用即可
 */
@Slf4j
//@Component
public class MessageDispatcher {
//public class MessageDispatcher implements ApplicationContextAware {



    private static final ConcurrentHashMap<BaseMessage.BaseMessagePkg.PayloadCase, ProtoBufProcessHandler> PROTO_BUF_HANDLERS =
            new ConcurrentHashMap<>();


    public static void registerHandler(List<ProtoBufProcessHandler> handlers){
        if (CollectionUtil.isNotEmpty(handlers)){
            handlers.stream()
                .filter(e->e.type()!=null)
                .forEach(handImpl-> {
                    log.debug("register handler {}",handImpl.type());
                    PROTO_BUF_HANDLERS.putIfAbsent(handImpl.type(),handImpl);

                });
        }
    }


    /**
     * dispatch message
     * {@link ProtoBufProcessHandler protobufMessage handler}
     * @param inbound
     * @param outbound
     * @param baseMessagePkg
     */
    public void dispatchMessage(NettyInbound inbound , NettyOutbound outbound, BaseMessage.BaseMessagePkg baseMessagePkg) {

        try{

            var payloadCase = baseMessagePkg.getPayloadCase();

            inbound.withConnection(connection -> {

                PROTO_BUF_HANDLERS.get(payloadCase).process(connection,baseMessagePkg);

            });

        }catch (Exception ex){
            log.error("Illegal message " ,ex);
        }
    }

    private enum SingleInstance{
        INSTANCE;
        private final MessageDispatcher instance;
        SingleInstance(){
            instance = new MessageDispatcher();
        }
        private MessageDispatcher getInstance(){
            return instance;
        }
    }
    public static MessageDispatcher getInstance(){
        return SingleInstance.INSTANCE.getInstance();
    }
    private MessageDispatcher(){}



}
