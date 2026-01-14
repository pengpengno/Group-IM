package com.github.im.server.config.ai;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({MultiAiConfig.class, AiSelectionConfig.class})
public class AiConfig {
    // 组合AI配置类，便于在应用中一次性导入所有AI相关配置
}