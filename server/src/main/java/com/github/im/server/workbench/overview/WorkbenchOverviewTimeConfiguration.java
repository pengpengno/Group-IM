package com.github.im.server.workbench.overview;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class WorkbenchOverviewTimeConfiguration {

    @Bean
    public Clock workbenchClock() {
        return Clock.systemDefaultZone();
    }
}
