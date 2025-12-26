package com.github.im.group.gui.config;

import com.github.im.common.connect.connection.server.MessageDispatcher;
import com.github.im.common.connect.connection.server.ProtoBufProcessHandler;
import com.github.im.common.connect.connection.server.ReactiveProtoBufProcessHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/1/9
 */

@RequiredArgsConstructor
@Component
@Slf4j
public class MessageDispatchInit implements ApplicationContextAware {

    private final List<ProtoBufProcessHandler> protoBufProcessHandlers;
    private final List<ReactiveProtoBufProcessHandler> reactiveProtoBufProcessHandlers;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        MessageDispatcher.registerHandler(protoBufProcessHandlers);
        MessageDispatcher.registerReactiveHandler(reactiveProtoBufProcessHandlers);
        log.info("Registered {} sync handlers and {} reactive handlers", 
                protoBufProcessHandlers.size(), reactiveProtoBufProcessHandlers.size());

    }
}