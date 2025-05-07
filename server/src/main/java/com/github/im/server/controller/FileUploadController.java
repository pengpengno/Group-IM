package com.github.im.server.controller;

import com.github.im.dto.file.ChunkCheckResponse;
import com.github.im.dto.file.FileUploadResponse;
import com.github.im.dto.session.FileMeta;
import com.github.im.server.model.FileResource;
import com.github.im.server.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    /**
     * 单文件直接上传接口（小文件适用）
     */
    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> upload(@RequestParam("file") MultipartFile file,
                                                     @RequestParam("uploaderId") UUID uploaderId) throws IOException {
        var fileInfo = fileStorageService.storeFile(file, uploaderId);

//        var fileMeta = FileMeta.builder()
//                .filename(fileInfo.getOriginalName())
//                .fileSize(fileInfo.getSize())
//                .contentType(fileInfo.getContentType())
//                .hash(fileInfo.getHash())
//                .build();

        return ResponseEntity.ok(fileInfo);
    }

    /**
     * 上传文件分片（支持断点续传）
     */
    @PostMapping("/upload/chunk")
    public ResponseEntity<Void> uploadChunk(@RequestParam("file") MultipartFile file,
                                            @RequestParam("fileHash") String fileHash,
                                            @RequestParam("chunkIndex") Integer chunkIndex,
                                            @RequestParam("totalChunks") Integer totalChunks,
                                            @RequestParam("uploaderId") UUID uploaderId) throws IOException {
        fileStorageService.uploadChunk(file, fileHash, chunkIndex, totalChunks, uploaderId);
        return ResponseEntity.ok().build();
    }

    /**
     * 分片上传完成后合并
     */
    @PostMapping("/upload/merge")
    public ResponseEntity<FileUploadResponse> merge(@RequestParam("fileHash") String fileHash,
                                                    @RequestParam("fileName") String fileName,
                                                    @RequestParam("uploaderId") UUID uploaderId) throws IOException {
        var file = fileStorageService.mergeChunks(fileHash, fileName, uploaderId);
        return ResponseEntity.ok(file);
    }

    /**
     * 查询已上传的分片，用于断点续传检查
     */
    @GetMapping("/upload/check")
    @SneakyThrows
    public ResponseEntity<ChunkCheckResponse> check(@RequestParam("fileHash") String fileHash) {
        List<Integer> uploadedChunks = fileStorageService.getUploadedChunks(fileHash);
        return ResponseEntity.ok(new ChunkCheckResponse(fileHash, uploadedChunks));
    }

    @GetMapping("/download/{fileId}")
    @PreAuthorize("isAuthenticated()") // 仅登录用户可下载
    public ResponseEntity<byte[]> downloadFile(@PathVariable UUID fileId) throws IOException {
        FileResource fileResource = fileStorageService.getFile(fileId);

        // 读取文件字节
        byte[] fileData = fileStorageService.loadFileAsBytes(fileResource);

        var originalName = fileResource.getOriginalName();
        String encodedFilename = URLEncoder.encode(originalName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        // 设置响应头
        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=\"file\"; filename*=UTF-8''" + encodedFilename)
                .header("Content-Type", fileResource.getContentType())
                .body(fileData);
    }

}
