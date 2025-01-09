package com.github.im.common.reactor.sink;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/1/8
 */
public class SinkTest {



    // 创建一个 Sinks，允许多个订阅者接收相同的消息
    private static final Sinks.Many<String> multicastSink = Sinks.many().multicast().onBackpressureBuffer();


    @Test
    void mulitcast() {
        Sinks.Many<String> multicastSink = Sinks.many().multicast().onBackpressureBuffer();

        // 订阅者 1
        multicastSink.asFlux().subscribe(msg -> System.out.println("Subscriber 1 received: " + msg));

        // 订阅者 2
        multicastSink.asFlux().subscribe(msg -> System.out.println("Subscriber 2 received: " + msg));

        // 发布消息
        multicastSink.tryEmitNext("Hello, subscribers!");


    }

    @Test
    @DisplayName("单播")
    void unicast() {


        Sinks.Many<String> unicastSink = Sinks.many().unicast().onBackpressureBuffer();

        // 订阅者 1
        unicastSink.asFlux().subscribe(msg -> System.out.println("Subscriber 1 received: " + msg));

        // 发布消息
        unicastSink.tryEmitNext("Hello, subscriber!");

    }

    @Test
    void hmain () {
        // 启动多个客户端并订阅消息
        startClient("Client 1");
        startClient("Client 2");
        startClient("Client 3");

        // 模拟服务端投放消息
        publishMessage("Hello, Clients!");
        publishMessage("How are you?");
    }


    // 模拟客户端订阅消息
    private static void startClient(String clientName) {
        multicastSink.asFlux()
                .doOnSubscribe(subscription -> System.out.println(clientName + " has subscribed."))
                .doOnTerminate(() -> System.out.println(clientName + " has unsubscribed."))
                .subscribe(message -> System.out.println(clientName + " received: " + message));
    }

    // 模拟服务端向客户端发送消息
    private static void publishMessage(String message) {
        Mono.just(message)
                .doOnTerminate(() -> System.out.println("Message '" + message + "' has been published."))
                .subscribe(msg -> multicastSink.tryEmitNext(msg));
    }
}