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
public class FileMetaController {

    private final FileStorageService fileStorageService;

    /**
     * 获取文件的元信息数据
     */
    @GetMapping("/meta")
    public ResponseEntity<FileMeta> getMeta(
                                                     @RequestParam("fileId") UUID fileId
    ) throws IOException {
        var fileInfo = fileStorageService.getFileMeta(fileId);

        return ResponseEntity.ok(fileInfo);
    }


}
