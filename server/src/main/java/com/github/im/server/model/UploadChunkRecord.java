package com.github.im.server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "upload_chunk_record")
@Getter
@Setter
public class UploadChunkRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String fileHash;

    private Integer totalChunks;

    private Integer chunkIndex;

    private String tempPath;

    private UUID uploaderId;

    private boolean uploaded;

    private Long createTime = System.currentTimeMillis();
}
