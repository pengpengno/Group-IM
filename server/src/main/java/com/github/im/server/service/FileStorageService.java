package com.github.im.server.service;

import cn.hutool.core.io.file.FileNameUtil;
import com.github.im.server.config.FileUploadProperties;
import com.github.im.server.model.FileResource;
import com.github.im.server.model.enums.FileStatus;
import com.github.im.server.model.enums.StorageType;
import com.github.im.server.repository.FileResourceRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileUploadProperties properties;
    private final FileResourceRepository repository;

    private Path baseDir;
    private Path chunkTempDir;

    @PostConstruct
    public void init() throws IOException {
        // 解析 basePath
        baseDir = resolvePath(properties.getBasePath());
        chunkTempDir = resolvePath(properties.getChunkTempPath());

        // 创建根目录
        Files.createDirectories(baseDir);
        Files.createDirectories(chunkTempDir);
    }

    /**
     * 存储单文件（小文件直传）
     */
    public FileResource storeFile(MultipartFile file, UUID uploaderId) throws IOException {
        String originalName = file.getOriginalFilename();
        String ext = FileNameUtil.extName(originalName);
        String contentType = file.getContentType();
        long size = file.getSize();
        String hash;
        try (InputStream is = file.getInputStream()) {
            hash = DigestUtils.md5DigestAsHex(is);
        }

        // 构建相对存储路径：yyyy/MM/dd/uuid.ext
        String relative = DateTimeFormatter.ofPattern("yyyy/MM/dd")
                .format(LocalDate.now())
                + "/" + UUID.randomUUID() + "." + ext;

        Path target = baseDir.resolve(relative).normalize();
        Files.createDirectories(target.getParent());
        file.transferTo(target);

        FileResource info = new FileResource();
        info.setOriginalName(originalName);
        info.setExtension(ext);
        info.setContentType(contentType);
        info.setSize(size);
        info.setStoragePath(relative);
        info.setStorageType(StorageType.LOCAL);
        info.setHash(hash);
        info.setUploadTime(Instant.now());
        info.setUploaderId(uploaderId);
        info.setStatus(FileStatus.NORMAL);

        return repository.save(info);
    }

    /**
     * 存储分片
     */
    public void uploadChunk(MultipartFile file, String fileHash, int chunkIndex,
                            int totalChunks, UUID uploaderId) throws IOException {
        Path sessionDir = chunkTempDir.resolve(fileHash).normalize();
        Files.createDirectories(sessionDir);

        // 分片文件名：00001.part
        String partName = String.format("%05d.part", chunkIndex);
        Path chunkPath = sessionDir.resolve(partName);
        file.transferTo(chunkPath);
    }

    /**
     * 获取已上传分片索引列表
     */
    @SneakyThrows
    public List<Integer> getUploadedChunks(String fileHash) throws IOException {
        Path sessionDir = chunkTempDir.resolve(fileHash).normalize();
        if (!Files.exists(sessionDir)) {
            return List.of();
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(sessionDir, "*.part")) {
            return
                    StreamSupport.stream(ds.spliterator(), false)
                            .map(p -> FileNameUtil.getName(p.getFileName().toString()))
                            .map(Integer::valueOf)
                            .sorted()
                            .collect(Collectors.toList());
        }
    }

    @SneakyThrows
    public FileResource getFile(UUID fileId) {
        return repository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found: " + fileId));
    }

    /**
     * 读取文件内容，storagePath 是类似 "2025/04/25/uuid.ext" 的相对路径
     */
    public byte[] loadFileAsBytes(FileResource fileResource) throws IOException {
        // 将相对路径拼到 baseDir
        Path filePath = baseDir.resolve(fileResource.getStoragePath()).normalize();
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new FileNotFoundException("文件不存在或不可读: " + filePath);
        }
        return Files.readAllBytes(filePath);
    }

    /**
     * 如果你在 Controller 中需要返回 Resource，可以这样：
     */
    @SneakyThrows
    public Resource loadFileAsResource(FileResource fileResource) throws MalformedURLException {
        Path filePath = baseDir.resolve(fileResource.getStoragePath()).normalize();
        UrlResource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new FileNotFoundException("文件不存在或不可读: " + filePath);
        }
        return resource;
    }

    /**
     * 合并所有分片并保存为最终文件
     */
    public FileResource mergeChunks(String fileHash, String originalName, UUID uploaderId) throws IOException {
        String ext = FileNameUtil.extName(originalName);
        String contentType = Files.probeContentType(Paths.get(originalName));
        String relative = DateTimeFormatter.ofPattern("yyyy/MM/dd")
                .format(LocalDate.now())
                + "/" + UUID.randomUUID() + "." + ext;

        Path finalPath = baseDir.resolve(relative).normalize();
        Files.createDirectories(finalPath.getParent());

        Path sessionDir = chunkTempDir.resolve(fileHash).normalize();
        try (var out = Files.newOutputStream(finalPath, StandardOpenOption.CREATE)) {
            Files.list(sessionDir)
                    .filter(p -> p.getFileName().toString().endsWith(".part"))
                    .sorted()
                    .forEach(p -> {
                        try {
                            Files.copy(p, out);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }

        long size = Files.size(finalPath);
        String hash;
        try (InputStream is = Files.newInputStream(finalPath)) {
            hash = DigestUtils.md5DigestAsHex(is);
        }

        // 清理临时分片
        FileSystemUtils.deleteRecursively(sessionDir);

        FileResource info = new FileResource();
        info.setOriginalName(originalName);
        info.setExtension(ext);
        info.setContentType(contentType);
        info.setSize(size);
        info.setStoragePath(relative);
        info.setStorageType(StorageType.LOCAL);
        info.setHash(hash);
        info.setUploadTime(Instant.now());
        info.setUploaderId(uploaderId);
        info.setStatus(FileStatus.NORMAL);

        return repository.save(info);
    }

    /**
     * 将配置路径解析为绝对路径：如果已是绝对，则原样；否则以 user.dir 为基准
     */
    private Path resolvePath(String configured) {
        Path p = Paths.get(configured);
        if (!p.isAbsolute()) {
            p = Paths.get(System.getProperty("user.dir")).resolve(p);
        }
        return p.normalize();
    }
}
