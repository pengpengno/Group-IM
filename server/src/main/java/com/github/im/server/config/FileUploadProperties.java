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

    /**
     * Client upload optimization hints exposed to web/mobile clients.
     */
    private Upload upload = new Upload();

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

    @Data
    public static class Upload {
        /** Whether clients should try to compress large images before upload. */
        private boolean compressionEnabled = true;
        /** Minimum image size in KB before compression is attempted. */
        private int compressMinSizeKb = 350;
        /** Maximum long edge for client-side upload image resizing. */
        private int maxImageEdge = 1600;
        /** JPEG quality hint used by clients during upload compression. */
        private int jpegQuality = 82;
    }
}
