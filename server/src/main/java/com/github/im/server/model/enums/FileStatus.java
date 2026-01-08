package com.github.im.server.model.enums;

/**
 * 文件状态
 */
public enum FileStatus {
    UPLOADING,  // 上传中
    NORMAL,  // 正常
    DELETED,// 删除
    EXPIRED, // 过期
    FAILED,  // 上传失败
    CHUNK_UPLOADING, // 分块上传中

    ;
}