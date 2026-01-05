package com.github.im.server.service.storage;

import com.github.im.server.model.FileResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 文件存储策略接口
 */
public interface StorageStrategy {
    
    /**
     * 存储文件
     * @param file 要存储的文件
     * @param uploaderId 上传者ID
     * @param duration 文件时长（对媒体文件有效）
     * @return 存储后的文件资源信息
     */
    FileResource store(MultipartFile file, UUID uploaderId, Long duration) throws IOException;
    
    /**
     * 存储分片合并后的文件
     * @param fileHash 文件哈希
     * @param originalName 原始文件名
     * @param clientId 客户端ID
     * @param duration 文件时长
     * @param chunkTempDir 分片临时目录
     * @return 存储后的文件资源信息
     */
    FileResource storeMergedFile(String fileHash, String originalName, UUID clientId, Long duration, Path chunkTempDir) throws IOException;
    
    /**
     * 获取文件访问路径
     * @param fileResource 文件资源
     * @return 文件访问路径
     */
    String getAccessPath(FileResource fileResource);
    
    /**
     * 删除文件
     * @param fileResource 文件资源
     */
    void delete(FileResource fileResource) throws IOException;
    
    /**
     * 获取存储类型标识
     * @return 存储类型
     */
    String getStorageType();
}