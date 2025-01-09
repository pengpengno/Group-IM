package com.github.im.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 确保将你的 API 请求和静态资源路径区分开来
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}
