package com.github.im.common.connect.connection.server.websocket;

import com.github.im.common.connect.connection.server.ReactiveServer;
import com.github.im.common.connect.connection.server.tcp.ReactorTcpServer;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.net.InetSocketAddress;


@Slf4j
@Singleton
public class WebSocketServer implements ReactiveServer {


    @Override
    public ReactiveServer getInstance(ReactorTcpServer tcpServer) {
        return ReactiveServer.super.getInstance(tcpServer);
    }



    @Override
    public ReactiveServer init(InetSocketAddress address) {


        DisposableServer server = HttpServer.create()
                .port(address.getPort())
                .route(routes -> routes.ws("/websocket", (inbound, outbound) -> {
                    // 当有新的 WebSocket 连接时，打印日志
                    System.out.println("New WebSocket connection established");

                    // 处理接收到的消息，并原样返回
                    return outbound.sendString(
                            inbound.receive()
                                    .asString()
                                    .doOnNext(message -> System.out.println("Received message: " + message))
                    );
                }))
                .bindNow();

        // 等待服务器关闭
        server.onDispose()
                .block();

        return null;
    }

    @Override
    public ReactiveServer start() {


        return null;
    }

    @Override
    public void stop() {

    }
}
