package com.github.im.server;

import com.github.im.common.connect.connection.server.tcp.ReactorTcpServer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication(scanBasePackages = {"com.github.im.server", "com.github.im.common"})
@Slf4j
@EnableAsync
//@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class Application {

    public static void main(String[] args) {

        SpringApplication.run(Application.class);

    }


    @Component
    public static class ServerListener implements ApplicationListener<WebServerInitializedEvent> {

        @Override
        public void onApplicationEvent(WebServerInitializedEvent event) {
            int port = event.getWebServer().getPort();
            String serverAddress = "http://localhost:" + port;
            log.info("Server is running at: " + serverAddress);

        }
    }
}
