package com.github.im.group

import com.github.im.group.config.ConfigManager
import com.github.im.group.manager.AudioPlaybackManager
import org.koin.dsl.module

val commonModule = module {
    // 代理存储已在平台模块中提供（Android/Desktop）
    
    // 配置管理器
    single { ConfigManager(get()) }
    
    // 提供当前的 AppConfig
    single { get<ConfigManager>().currentConfig.value }
    
    // 音频播放管理器
    single { AudioPlaybackManager(get()) }

}