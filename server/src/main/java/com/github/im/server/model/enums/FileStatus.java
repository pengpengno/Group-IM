package com.github.im.server.model.enums;

/**
 * 文件状态
 */
public enum FileStatus {
    NORMAL,
    DELETED,
    EXPIRED,
    FAILED,
    CHUNK_UPLOADING, // 分块上传中

    ;
}