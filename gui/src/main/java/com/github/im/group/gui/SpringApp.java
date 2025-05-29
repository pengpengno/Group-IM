package com.github.im.group.gui;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/5/21
 */
// 不需要注入mvc
//@SpringBootApplication(exclude = {WebMvcAutoConfiguration.class})
@ComponentScan(basePackages = "com.github.im.group")
@Configuration
public class SpringApp {

    public static void main(String[] args) {
        SpringApplication.run(SpringApp.class, args);
    }
}