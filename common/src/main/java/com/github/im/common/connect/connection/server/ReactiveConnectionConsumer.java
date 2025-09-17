package com.github.im.common.connect.connection.server;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.github.im.common.connect.connection.ConnectionConsumer;
import com.github.im.common.connect.model.proto.BaseMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.netty.Connection;
import reactor.netty.NettyInbound;

import java.util.concurrent.ForkJoinPool;

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
//                    .receive()
//                    .asByteArray()
                    .receiveObject()
                    .cast(BaseMessage.BaseMessagePkg.class)
                    .handle( (obj,sink) -> {
                        try {
                            // 直接用 Protobuf 的 parseFrom 解码
//                            BaseMessage.BaseMessagePkg message = BaseMessage.BaseMessagePkg.parseFrom(obj);
                            log.debug("Received Protobuf Message: {}", obj);
                            MessageDispatcher.getInstance().dispatchMessage(nettyInbound, nettyOutbound, obj);

                        } catch (Exception e) {
                            log.error("解析失败: {}", e.getMessage());
                        }
                    })

                    ;


            var outbound = nettyOutbound.sendObject(Flux.concat(messageFlux));
            return outbound.then();

        });

        log.debug("The ReactorConnectionConsumer SPI FxReactiveClientHandler service provider has load ! ");
    }



    @Override
    public void accept(Connection c) {

        super.accept(c);

    }
}
