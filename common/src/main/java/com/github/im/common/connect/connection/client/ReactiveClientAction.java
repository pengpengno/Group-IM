package com.github.im.common.connect.connection.client;

import com.google.protobuf.Message;
import reactor.core.publisher.Mono;

/**
 *
 * user for client send message to server
 * @author pengpeng
 * @description
 * @date 2023/3/13
 */
public interface ReactiveClientAction {


    Mono<Void> sendMessage(Message message);


    Mono<Void> sendString(String message);


    @Deprecated
    default Mono<Void> sendObject(Object message){
        return Mono.empty();
    }

}
