package com.github.im.group.gui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;

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
@SpringBootApplication(exclude = {WebMvcAutoConfiguration.class})
public class SpringApp {

    public static void main(String[] args) {
        SpringApplication.run(SpringApp.class, args);
    }
}