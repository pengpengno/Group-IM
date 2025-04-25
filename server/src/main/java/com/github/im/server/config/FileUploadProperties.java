package com.github.im.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "file.upload")
@Data
public class FileUploadProperties {

    private String basePath; //文件存户路径
    private String chunkTempPath; // 分段上传文件存储路径
}
