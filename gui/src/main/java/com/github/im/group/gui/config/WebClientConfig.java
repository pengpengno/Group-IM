package com.github.im.group.gui.config;

import com.github.im.group.gui.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.netty.http.client.HttpClient;
import java.time.Duration;

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
    public WebClient webClient(@Autowired ServerConnectProperties serverConnectProperties, @Autowired WebClientFilter authFilter) {

        var httpClient = HttpClient.create()
//                .secure(SslProvider.defaultClientProvider()) // 启用 HTTPS
                .baseUrl(serverConnectProperties.getRest().getHost())
//                .protocol(HttpClient.H2) // 强制 HTTP/2
                .responseTimeout(Duration.ofSeconds(10));

        return   WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(authFilter)
                .build();

    }

    @Bean
//    @LoadBalanced
    public HttpServiceProxyFactory httpServiceProxyFactory(@Autowired WebClient webClient) {

        WebClientAdapter adapter = WebClientAdapter.create(webClient);

        return HttpServiceProxyFactory.builderFor(adapter).build();

    }

    /**
     *
     * 下面需要显示的声明定义 bean 不然再 graalvm 的静态编译中
     * spring-aot 不会 编译这类bean ,再启动graalvm 编译的 程序时候 会 无法找打这些bean
     * {@link HttpExchangeAutoRegister endpoint 注册器}  ,如上方式可以自动化的注册但是 无法 兼容 graalvm 编译的情况
     * 细节支持 有待研究
     */
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

    @Bean
    @ConditionalOnMissingBean(ConversationEndpoint.class)
    public ConversationEndpoint ConversationEndpoint(@Autowired HttpServiceProxyFactory webClient) {
        return webClient.createClient(ConversationEndpoint.class);
    }

    @Bean
    @ConditionalOnMissingBean(FileEndpoint.class)
    public FileEndpoint fileEndpoint(@Autowired HttpServiceProxyFactory webClient) {
        return webClient.createClient(FileEndpoint.class);
    }




}