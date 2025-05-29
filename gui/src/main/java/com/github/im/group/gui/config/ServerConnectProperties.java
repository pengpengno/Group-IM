package com.github.im.group.gui.config;

import com.github.im.common.connect.connection.client.ClientLifeStyle;
import com.github.im.common.connect.connection.client.ClientToolkit;
import com.github.im.common.connect.connection.client.ReactiveClientAction;
import com.github.im.common.connect.connection.server.tcp.ReactorTcpServer;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.net.InetSocketAddress;

/**
 * Description:
 * <p>
 *     configure a long-lived client
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2024/10/18
 */
@Slf4j
@ConfigurationProperties(value = "im.server")
@Configuration
@EnableConfigurationProperties(ServerConnectProperties.class) // ✅ 加这一句
@Data
public class ServerConnectProperties  {

    // rest api  host
    private Rest rest;

    //  tcp connect host
    private Connect connect;
    @Data
    public static class Rest{
        String host;
    }
    @Data
    public static class Connect{

        public int port = 8088; //default 8088
        public String host ;

    }



}