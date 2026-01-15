package com.github.im.server.config.netty;

import com.github.im.common.connect.connection.server.ReactiveServer;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class NettyServerInitializer implements SmartLifecycle {

    private final ReactiveServer tcpServer;
    private ExecutorService executorService;

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void start() {
        log.info("start netty server");
        // 在单独的线程中启动Netty服务器，避免阻塞主线程
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(this::startNettyServerAsync);
        log.info("netty server start command issued");
    }

    @Override
    public void stop() {
        tcpServer.stop();
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Override
    public boolean isRunning() {
        return tcpServer.isRunning();
    }

    public void startNettyServerAsync() {
        try {
            tcpServer.start();
        } catch (Exception e) {
            log.error("Failed to start Netty server", e);
        }
    }

    @PreDestroy
    public void stopNettyServer() {
        tcpServer.stop();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}