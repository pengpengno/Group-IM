package com.github.im.dto.file;

import lombok.Data;

/**
 * 文件上传
 */
@Data
public class UploadFileRequest {


    /**
     * 文件大小
     */
    private Long size;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件时长（媒体文件）
     */
    private Long duration;
}
