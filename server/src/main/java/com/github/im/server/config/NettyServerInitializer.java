package com.github.im.server.config;

import com.github.im.common.connect.connection.server.ProtoBufProcessHandler;
import com.github.im.common.connect.connection.server.ReactiveServer;
import com.github.im.common.connect.connection.server.tcp.ReactorTcpServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.net.InetSocketAddress;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NettyServerInitializer  implements SmartLifecycle {

    private final ReactiveServer tcpServer;


    @Override
    public boolean isAutoStartup() {
        return true;
    }


    @Override
    public void start() {
        log.info("start netty server");
//        new Thread(()->startNettyServerAsync() ).start();
        startNettyServerAsync();
        log.info("start netty server succ");

    }

    @Override
    public void stop() {
        tcpServer.stop();
    }

    @Override
    public boolean isRunning() {
        return tcpServer.isRunning();
    }

    @Async
    public void startNettyServerAsync() {
        tcpServer.start();
    }

    @PreDestroy
    public void stopNettyServer() {
        tcpServer.stop();
    }
}
