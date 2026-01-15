package com.github.im.server.controller;

import com.github.im.dto.file.ChunkCheckResponse;
import com.github.im.dto.file.FileUploadResponse;
import com.github.im.dto.file.UploadFileRequest;
import com.github.im.server.model.FileResource;
import com.github.im.server.model.enums.FileStatus;
import com.github.im.server.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    /**
     * 预创建文件记录接口，获取文件ID
     */
    @PostMapping("/uploadId")
    public ResponseEntity<FileUploadResponse> uploadId(
            @RequestBody UploadFileRequest request
    ) throws IOException {
        // 创建一个预占的文件记录，返回文件ID供后续上传使用
        FileUploadResponse response = fileStorageService.createFilePlaceholder(request);
        log.info("获取文件Id 成功：{}", response);

        return ResponseEntity.ok(response);
    }

    /**
     * 单文件直接上传接口（小文件适用）
     */
    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> upload(@RequestParam("file") MultipartFile file,
                                                     @RequestParam("fileId") UUID fileId,
                                                     @RequestParam(required = false) Long duration   // 音频时长（媒体相关数据信息将存储在 MediaFileResource 中）
    ) throws IOException {
        var fileInfo = fileStorageService.storeFile(file, fileId,duration);
        log.info("文件上传成功：{}", fileInfo);
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
                                            @RequestParam("fileId") UUID fileId) throws IOException {
        fileStorageService.uploadChunk(file, fileHash, chunkIndex, totalChunks, fileId);
        return ResponseEntity.ok().build();
    }

    /**
     * 分片上传完成后合并
     */
    @PostMapping("/upload/merge")
    public ResponseEntity<FileUploadResponse> merge(@RequestParam("fileHash") String fileHash,
                                                    @RequestParam("fileName") String fileName,
                                                    @RequestParam("fileId") UUID fileId,
                                                    @RequestParam(value = "duration", required = false) Long duration // 音频时长（媒体相关数据信息将存储在 MediaFileResource 中）
    ) throws IOException {
        var file = fileStorageService.mergeChunks(fileHash, fileName, fileId, duration);
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
    public ResponseEntity<ResourceRegion> downloadFile(@PathVariable UUID fileId,
                                                 @RequestHeader(value = "Range", required = false) String rangeHeader) throws IOException {


        FileResource fileResource = fileStorageService.getFile(fileId);
        if (fileResource == null ) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (fileResource.getStatus() != FileStatus.NORMAL) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        var file = fileStorageService.loadFile(fileResource);
        var resource = new UrlResource(file.toURI());

        long contentLength = resource.contentLength();
        ResourceRegion region;

        if (rangeHeader != null) {
            List<HttpRange> httpRanges = HttpRange.parseRanges(rangeHeader);
            HttpRange httpRange = httpRanges.get(0);
            long start = httpRange.getRangeStart(contentLength);
            long end = httpRange.getRangeEnd(contentLength);
            long rangeLength = end - start + 1;
            region = new ResourceRegion(resource, start, rangeLength);
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaType.parseMediaType(fileResource.getContentType()))
                    .body(region);
        } else {
            region = new ResourceRegion(resource, 0, contentLength);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(fileResource.getContentType()))
                    .body(region);
        }

    }

}
