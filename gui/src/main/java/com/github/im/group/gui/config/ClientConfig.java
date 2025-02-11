package com.github.im.group.gui.config;

import com.github.im.common.connect.connection.client.ClientToolkit;
import com.github.im.common.connect.connection.client.ReactiveClientAction;
import com.github.im.group.gui.api.UserEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/1/15
 */

@Configuration
@Slf4j
public class ClientConfig {


    @Bean()
    @ConditionalOnBean(ServerConnectProperties.class)
    public ReactiveClientAction reactiveClientAction()
    {
        return ClientToolkit.reactiveClientAction();
    }




}