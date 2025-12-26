package com.github.im.common.connect.connection.server;

import com.github.im.common.connect.model.proto.BaseMessage;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;

/***
 * 响应式连接消息处理器接口
 * 用于处理异步非阻塞的消息处理操作
 */
@FunctionalInterface
public interface ReactiveProtoBufProcessHandler {

    /***
     * 获取对应的消息类型
     * @return 返回业务对应的类型
     */
    default BaseMessage.BaseMessagePkg.PayloadCase type(){
        return BaseMessage.BaseMessagePkg.PayloadCase.PAYLOAD_NOT_SET;
    }

    /***
     * 异步处理客户端网络IO
     * @param con 客户端连接
     * @param message IO数据
     * @return Mono<Void> 表示处理完成的响应式流
     */
    Mono<Void> process(@Nullable Connection con , BaseMessage.BaseMessagePkg message);
}