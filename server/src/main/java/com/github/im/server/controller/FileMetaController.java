package com.github.im.server.controller;

import com.github.im.dto.message.FileMeta;
import com.github.im.server.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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
