package com.github.im.group.gui.config;

import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.api.FriendShipEndpoint;
import com.github.im.group.gui.api.UserEndpoint;
import com.github.im.group.gui.context.UserInfoContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Mono;

/**
 * Description:
 * <p>
 *     auth filter ,all request should try to get token if {@link UserInfoContext }  not null ,and when login callback ,
 *     request add current user corresponding token to header
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2024/10/17
 */
@Component
public class WebClientFilter implements ExchangeFilterFunction {


    private UserInfo currentUser ;


    public WebClientFilter() {
        UserInfoContext.subscribeUserInfoSink().subscribe(userInfo -> {
            currentUser = userInfo;
        });
    }
    @Override
    public ExchangeFilterFunction andThen(ExchangeFilterFunction afterFilter) {
        return ExchangeFilterFunction.super.andThen(afterFilter);
    }

    @Override
    public ExchangeFunction apply(ExchangeFunction exchange) {
        return ExchangeFilterFunction.super.apply(exchange);
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {

//        var currentUser = UserInfoContext.getCurrentUser();// 从上下文获取 token

        if (currentUser != null) {
            var token = currentUser.getToken();
            if (StringUtils.hasText(token)){
                request = ClientRequest.from(request)
                        .header("Authorization", "Bearer "+ token)
                        .build();
            }

        }
        return next.exchange(request);

    }



}