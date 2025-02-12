package com.github.im.group.gui.config;

import com.github.im.group.gui.api.FriendShipEndpoint;
import com.github.im.group.gui.api.GroupMemberEndpoint;
import com.github.im.group.gui.api.MessageEndpoint;
import com.github.im.group.gui.api.UserEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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


    @Bean
    @ConditionalOnMissingBean(UserEndpoint.class)
    public UserEndpoint userEndpoint(@Autowired HttpServiceProxyFactory webClient) {
        return webClient.createClient(UserEndpoint.class);
    }


    @Bean
    @ConditionalOnMissingBean(GroupMemberEndpoint.class)
    public GroupMemberEndpoint groupMemberEndpoint(@Autowired HttpServiceProxyFactory webClient) {
        return webClient.createClient(GroupMemberEndpoint.class);
    }

    @Bean
    @ConditionalOnMissingBean(MessageEndpoint.class)
    public MessageEndpoint MessageEndpoint(@Autowired HttpServiceProxyFactory webClient) {
        return webClient.createClient(MessageEndpoint.class);
    }

    @Bean
    @ConditionalOnMissingBean(FriendShipEndpoint.class)
    public FriendShipEndpoint FriendShipEndpoint(@Autowired HttpServiceProxyFactory webClient) {
        return webClient.createClient(FriendShipEndpoint.class);
    }



}