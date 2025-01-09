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
            Flux<BaseMessage.BaseMessagePkg> handleBaseMessage = nettyInbound.receiveObject()
                    .cast( BaseMessage.BaseMessagePkg.class)
                    .handle((obj, sink) -> {
                log.debug("Received Object: {}", obj);
                if (obj != null) {
                    // 处理 BaseMessage 类型
                    MessageDispatcher.getInstance().dispatchMessage(nettyInbound ,nettyOutbound,obj);

                }

            });


//            var outbound = nettyOutbound.sendByteArray(Flux.concat(handle));
            var outbound = nettyOutbound.sendObject(Flux.concat(handleBaseMessage));

            return outbound.then();

        });

        log.debug("The ReactorConnectionConsumer SPI FxReactiveClientHandler service provider has load ! ");
    }



    @Override
    public void accept(Connection c) {

        super.accept(c);

    }
}
