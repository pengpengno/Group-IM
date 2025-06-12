package com.github.im.common.connect.connection.server;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.github.im.common.connect.connection.ConnectionConsumer;
import com.github.im.common.connect.model.proto.BaseMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.netty.Connection;
import reactor.netty.NettyInbound;

/**
 * 响应式 服务处理 Handler
 * @author pengpeng
 * @description
 * @date 2023/3/14
 */
@Slf4j
public class ReactiveConnectionConsumer extends ConnectionConsumer {


    @SneakyThrows
    public ReactiveConnectionConsumer(){
        super((nettyInbound, nettyOutbound) -> {
            // 处理 BaseMessage（对象）
            var messageFlux = nettyInbound
                    .receive()
                    .asByteArray()
                    .map(bytes -> {
                        try {
                            // 直接用 Protobuf 的 parseFrom 解码
                            BaseMessage.BaseMessagePkg message = BaseMessage.BaseMessagePkg.parseFrom(bytes);
                            log.debug("Received Protobuf Message: {}", message);
                            return message;
                        } catch (Exception e) {
                            log.error("解析失败: {}", e.getMessage());
                            return null;
                        }
                    })
                    .filter(msg -> msg != null)
                    .doOnNext(msg -> {
//                    .subscribe(msg -> {
                        try {
                            MessageDispatcher.getInstance().dispatchMessage(nettyInbound, nettyOutbound, msg);
                        } catch (Exception e) {
                            log.error("消息处理异常: {}", e.getMessage());
                        }
                    });

//                    .subscribe();
//            Flux<BaseMessage.BaseMessagePkg> handleBaseMessage = nettyInbound.receiveObject()
////                    .cast( BaseMessage.BaseMessagePkg.class)
//                    .handle((obj, sink) -> {
//                        log.debug("Received Object: {}", obj);
//                        if (obj != null) {
//                            // 处理 BaseMessage 类型
//                            MessageDispatcher.getInstance().dispatchMessage(nettyInbound ,nettyOutbound, (BaseMessage.BaseMessagePkg) obj);
//
//                        }
//
//            });
//

//            var outbound = nettyOutbound.sendByteArray(Flux.concat(handle));
            var outbound = nettyOutbound.sendObject(Flux.concat(messageFlux));
//            var outbound = nettyOutbound.sendObject(Flux.concat(handleBaseMessage));

            return outbound.then();

        });

        log.debug("The ReactorConnectionConsumer SPI FxReactiveClientHandler service provider has load ! ");
    }



    @Override
    public void accept(Connection c) {

        super.accept(c);

    }
}
