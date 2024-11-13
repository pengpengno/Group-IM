package com.github.im.group.gui.config;

import com.github.im.group.gui.api.FriendShipEndpoint;
import com.github.im.group.gui.api.UserEndpoint;
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
//@EnableHttpExchangeProxy
@Enable
public class WebClientConfig {

    @Bean
    @ConditionalOnProperty(value = {"server.host"})
    @LoadBalanced
    public HttpServiceProxyFactory webClient(@Value("${server.host}")  String host) {
        var webClient = WebClient.builder()
                .baseUrl(host)
                .build();


        WebClientAdapter adapter = WebClientAdapter.create(webClient);

        return HttpServiceProxyFactory.builderFor(adapter).build();

    }


    @Bean
    @ConditionalOnBean(HttpServiceProxyFactory.class)
    public UserEndpoint userEndpoint (HttpServiceProxyFactory factory) {
        return factory.createClient(UserEndpoint.class);
    }

    @Bean
    public FriendShipEndpoint friendShipEndpoint(HttpServiceProxyFactory factory) {
        return factory.createClient(FriendShipEndpoint.class);
    }

}