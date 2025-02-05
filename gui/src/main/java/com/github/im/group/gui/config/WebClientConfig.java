package com.github.im.group.gui.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2024/10/17
 */

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public HttpServiceProxyFactory webClient(@Autowired ServerConnectProperties serverConnectProperties, @Autowired WebClientFilter authFilter) {
        var webClient = WebClient.builder()

                .baseUrl(serverConnectProperties.getRest().getHost())
                .filter(authFilter)
                .build();

        WebClientAdapter adapter = WebClientAdapter.create(webClient);

        return HttpServiceProxyFactory.builderFor(adapter).build();

    }



}