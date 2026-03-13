package com.github.im.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "group.file.upload")
@Data
public class FileUploadProperties {

    /**
     * 文件存储路径（支持相对路径和绝对路径）
     * 相对路径会以 user.dir 为基准
     */
    private String basePath;
    
    /**
     * 分片上传临时文件存储路径（支持相对路径和绝对路径）
     * 相对路径会以 user.dir 为基准
     */
    private String chunkTempPath;
}
