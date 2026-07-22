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

    /**
     * Preview image delivery settings used by /api/files/preview/{fileId}.
     */
    private Preview preview = new Preview();

    /**
     * Generated poster/thumbnail settings for audio and video media.
     */
    private Thumbnail thumbnail = new Thumbnail();

    @Data
    public static class Preview {
        /** Default width used when clients omit the width parameter. */
        private int defaultWidth = 480;
        /** Smallest preview width accepted from clients. */
        private int minWidth = 160;
        /** Largest preview width accepted from clients. */
        private int maxWidth = 1600;
        /** Default quality used when clients omit the quality parameter. */
        private int defaultQuality = 75;
        /** Smallest preview quality accepted from clients. */
        private int minQuality = 40;
        /** Largest preview quality accepted from clients. */
        private int maxQuality = 95;
    }

    @Data
    public static class Thumbnail {
        /** Enables derived poster thumbnails for audio/video files. */
        private boolean enabled = true;
        /** Output width of generated poster thumbnails. */
        private int width = 640;
        /** Output height of generated poster thumbnails. */
        private int height = 360;
        /** Output quality of generated poster thumbnails. */
        private int quality = 82;
    }
}
