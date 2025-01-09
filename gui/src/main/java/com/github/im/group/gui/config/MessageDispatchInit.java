package com.github.im.group.gui.config;

import com.github.im.common.connect.connection.server.MessageDispatcher;
import com.github.im.common.connect.connection.server.ProtoBufProcessHandler;
import lombok.RequiredArgsConstructor;
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
public class MessageDispatchInit implements ApplicationContextAware {

    private final List<ProtoBufProcessHandler> protoBufProcessHandlers;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        MessageDispatcher.registerHandler(protoBufProcessHandlers);

    }
}