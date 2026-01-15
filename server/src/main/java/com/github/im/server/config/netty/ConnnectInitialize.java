package com.github.im.server.config.netty;

import com.github.im.common.connect.connection.server.ReactiveServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Description:
 * <p>
 *     长连接初始化
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2024/10/18
 */
@Slf4j
@RequiredArgsConstructor
public class ConnnectInitialize implements ApplicationContextAware {


    private final ReactiveServer reactiveServer;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        reactiveServer.start();
    }
}