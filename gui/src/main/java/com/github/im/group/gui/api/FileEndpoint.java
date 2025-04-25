package com.github.im.group.gui.api;

import com.github.im.dto.file.ChunkCheckResponse;
import com.github.im.dto.file.FileUploadResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * 文件上传端点（支持分片）
 */
@HttpExchange("/api/files")
public interface FileEndpoint {

    // 单文件上传
    @PostExchange(value = "/upload", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
//    Mono<FileUploadResponse> upload(@RequestPart("file") MultipartFile file,
    Mono<FileUploadResponse> upload(@RequestPart("file") Resource file,
                                    @RequestParam("uploaderId") UUID uploaderId);
    // 分片上传
    @PostExchange(value = "/upload/chunk", contentType = "multipart/form-data")
    Mono<Void> uploadChunk(@RequestParam("file") MultipartFile file,
                           @RequestParam("fileHash") String fileHash,
                           @RequestParam("chunkIndex") Integer chunkIndex,
                           @RequestParam("totalChunks") Integer totalChunks,
                           @RequestParam("uploaderId") UUID uploaderId);

    // 分片合并
    @PostExchange("/upload/merge")
    Mono<FileUploadResponse> mergeChunks(@RequestParam("fileHash") String fileHash,
                                         @RequestParam("fileName") String fileName,
                                         @RequestParam("uploaderId") UUID uploaderId);

    // 上传前检查（返回已上传分片索引）
    @PostExchange("/upload/check")
    Mono<ChunkCheckResponse> checkChunks(@RequestParam("fileHash") String fileHash);



    // 上传前检查（返回已上传分片索引）
//    @PostExchange(value = "/download/{fileId}", accept = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @GetExchange(value = "/download/{fileId}", accept = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    Mono<ResponseEntity<Resource>> downloadFile(@PathVariable("fileId") UUID fileId);

}
