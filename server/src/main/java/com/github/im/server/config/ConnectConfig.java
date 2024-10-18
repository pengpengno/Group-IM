package com.github.im.server.config;

import com.github.im.common.connect.connection.server.ReactiveServer;
import com.github.im.common.connect.connection.server.tcp.ReactorTcpServer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2024/10/18
 */
@Configuration
public class ConnectConfig  {

    @Value("${tcp.port:8088}")
    public int tcpPort = 8088;

    @Bean
    public ReactiveServer reactiveServer () {

        var instance = ReactorTcpServer.getInstance();
        instance.init(new InetSocketAddress("localhost", tcpPort))
                ;
        return  instance;
    }


}