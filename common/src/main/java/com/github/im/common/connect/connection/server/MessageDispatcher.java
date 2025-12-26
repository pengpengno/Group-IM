package com.github.im.common.connect.connection.server;

import cn.hutool.core.collection.CollectionUtil;
import com.github.im.common.connect.model.proto.BaseMessage;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
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
public class MessageDispatcher {



    private static final ConcurrentHashMap<BaseMessage.BaseMessagePkg.PayloadCase, ProtoBufProcessHandler> PROTO_BUF_HANDLERS =
            new ConcurrentHashMap<>();
    
    private static final ConcurrentHashMap<BaseMessage.BaseMessagePkg.PayloadCase, ReactiveProtoBufProcessHandler> REACTIVE_PROTO_BUF_HANDLERS =
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
    
    public static void registerReactiveHandler(List<ReactiveProtoBufProcessHandler> handlers){
        if (CollectionUtil.isNotEmpty(handlers)){
            handlers.stream()
                .filter(e->e.type()!=null)
                .forEach(handImpl-> {
                    log.debug("register reactive handler {}",handImpl.type());
                    REACTIVE_PROTO_BUF_HANDLERS.putIfAbsent(handImpl.type(),handImpl);

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
    
    /**
     * 响应式分发消息
     */
    public Mono<Void> dispatchMessageReactive(NettyInbound inbound, NettyOutbound outbound, BaseMessage.BaseMessagePkg baseMessagePkg) {
        return Mono.defer(() -> {
            try {
                var payloadCase = baseMessagePkg.getPayloadCase();

                // 首先尝试使用响应式处理器
                var reactiveHandler = REACTIVE_PROTO_BUF_HANDLERS.get(payloadCase);
                if (reactiveHandler != null) {
                    return Mono.fromSupplier(()-> {
                        return inbound.withConnection(connection ->
                                reactiveHandler.process(connection, baseMessagePkg)
                                .subscribeOn(Schedulers.boundedElastic())

                        );
                    }).then();

                }

                // 如果没有响应式处理器，使用传统处理器并包装为 Mono
                var syncHandler = PROTO_BUF_HANDLERS.get(payloadCase);
                if (syncHandler != null) {

                    return Mono.fromSupplier(()-> {
                        return inbound.withConnection(connection ->
                                Mono.fromRunnable(() -> syncHandler.process(connection, baseMessagePkg))
                                        .subscribeOn(Schedulers.boundedElastic())
                        );
                    }).then();
//                    return inbound.withConnection(connection ->
//                        Mono.fromRunnable(() -> syncHandler.process(connection, baseMessagePkg))
//                    );
                } else {
                    log.warn("No handler found for payload case: {}", payloadCase);
                    return Mono.empty();
                }
            } catch (Exception ex) {
                log.error("Illegal message in reactive dispatch", ex);
                                    return Mono.empty();

//                return Mono.error(ex);
            }
        });
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