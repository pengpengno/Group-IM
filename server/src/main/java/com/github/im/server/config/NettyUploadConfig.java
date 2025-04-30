package com.github.im.server.config;

import io.netty.handler.codec.http.HttpObjectAggregator;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.server.HttpServer;

@Configuration
public class NettyUploadConfig {

    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> nettyCustomizer() {
        return factory -> factory.addServerCustomizers(httpServer -> 
            httpServer
                .httpRequestDecoder(spec -> spec.maxInitialLineLength(8192)
                                                .maxHeaderSize(8192)
                                                .maxChunkSize(10 * 1024 * 1024)) // 限制单个 chunk 大小
                .doOnConnection(conn -> conn
                    .addHandlerLast(new HttpObjectAggregator(20 * 1024 * 1024)) // 限制总请求体积为 20MB
                )
        );
    }
}
