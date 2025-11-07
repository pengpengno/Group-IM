package com.github.im.server.controller;

import com.github.im.dto.file.ChunkCheckResponse;
import com.github.im.dto.file.FileUploadResponse;
import com.github.im.dto.session.FileMeta;
import com.github.im.server.model.FileResource;
import com.github.im.server.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
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
                                                     @RequestParam("clientId") UUID clientId,
                                                     @RequestParam() Long duration   // 音频时长
    ) throws IOException {
        var fileInfo = fileStorageService.storeFile(file, clientId,duration);

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
                                            @RequestParam("clientId") UUID clientId) throws IOException {
        fileStorageService.uploadChunk(file, fileHash, chunkIndex, totalChunks, clientId);
        return ResponseEntity.ok().build();
    }

    /**
     * 分片上传完成后合并
     */
    @PostMapping("/upload/merge")
    public ResponseEntity<FileUploadResponse> merge(@RequestParam("fileHash") String fileHash,
                                                    @RequestParam("fileName") String fileName,
                                                    @RequestParam("clientId") UUID clientId) throws IOException {
        var file = fileStorageService.mergeChunks(fileHash, fileName, clientId);
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


        // TODO  根据 当前用户判断是否有权限下载文件
        FileResource fileResource = fileStorageService.getFile(fileId);
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
