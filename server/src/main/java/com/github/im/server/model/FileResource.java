package com.github.im.server.model;

import com.github.im.server.model.enums.FileStatus;
import com.github.im.server.model.enums.StorageType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "file_resource")
@Data
public class FileResource {

    @Id
    private UUID id;

    private String originalName;

    private String extension;

    private String contentType;

    private long size;

    private String storagePath;
    /**
     * 文件时长  ， 适用于 视频 / 音频文件 单位为秒
     */
    private Long duration;

    @Enumerated(EnumType.STRING)
    private StorageType storageType;

    private String hash;

    private Instant uploadTime;
    /**
     * 上传时候 客户端生成的 clientId ,  用当文件过大时， 会等待所有文件上传完毕
     * 进行merge chunk 之后， 再 在数据库插入最新chunk
     */
    private UUID clientId;


    @Enumerated(EnumType.STRING)
    private FileStatus status;

    private String remark;




}
