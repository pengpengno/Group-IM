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
    @GeneratedValue
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

    private UUID clientId;
    private UUID uploaderId;


    @Enumerated(EnumType.STRING)
    private FileStatus status;

    private String remark;




}
