package com.github.im.server.service.storage;

import cn.hutool.core.io.file.FileNameUtil;
import com.github.im.server.model.FileResource;
import com.github.im.server.model.enums.FileStatus;
import com.github.im.server.model.enums.StorageType;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Function;

/**
 * 本地文件存储策略实现
 */
public class LocalStorageStrategy implements StorageStrategy {
    
    private final Path baseDir;
    private final Function<MultipartFile, String> hashFunction;
    
    public LocalStorageStrategy(Path baseDir, Function<MultipartFile, String> hashFunction) {
        this.baseDir = baseDir;
        this.hashFunction = hashFunction;
    }
    
    @Override
    public FileResource store(MultipartFile file, UUID fileId, Long duration) throws IOException {


        String originalName = file.getOriginalFilename();
        String ext = FileNameUtil.extName(originalName);
        String contentType = file.getContentType();
        
        long size = file.getSize();
        String hash = hashFunction.apply(file);
        
        // 构建相对存储路径：yyyy/MM/dd/uuid.ext
        String relative = buildStoragePath(ext);
        
        Path target = baseDir.resolve(relative).normalize();
        Files.createDirectories(target.getParent());
        file.transferTo(target);
        
        FileResource info = new FileResource();
        info.setId(fileId); //
        info.setOriginalName(originalName);
        info.setExtension(ext);
        info.setContentType(contentType);
        info.setSize(size);
        info.setStoragePath(relative);
        info.setStorageType(StorageType.LOCAL);
        info.setHash(hash);
        info.setUploadTime(LocalDateTime.now());
        info.setStatus(FileStatus.NORMAL);

        return info;
    }
    
    @Override
    public FileResource storeMergedFile(String fileHash, String originalName, UUID clientId, Long duration, Path chunkTempDir) throws IOException {
        String ext = FileNameUtil.extName(originalName);
        String contentType = Files.probeContentType(Paths.get(originalName));
        
        // 构建相对存储路径：yyyy/MM/dd/uuid.ext
        String relative = buildStoragePath(ext);

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
                            throw new RuntimeException("Failed to merge file chunks", e);
                        }
                    });
        }

        long size = Files.size(finalPath);
        String hash;
        try (InputStream is = Files.newInputStream(finalPath)) {
            hash = calculateFileHash(is);
        }

        // 清理临时分片
        FileSystemUtils.deleteRecursively(sessionDir.toFile());

        FileResource info = new FileResource();
        info.setId(UUID.randomUUID()); // 设置预先生成的ID
        info.setOriginalName(originalName);
        info.setExtension(ext);
        info.setContentType(contentType);
        info.setSize(size);
        info.setStoragePath(relative);
        info.setStorageType(StorageType.LOCAL);
        info.setHash(hash);
        info.setUploadTime(LocalDateTime.now());
        info.setStatus(FileStatus.NORMAL);
        // 已废弃：媒体相关数据信息应存储在 MediaFileResource 中，而不是 FileResource
        // info.setDuration(duration);

        return info;
    }
    
    @Override
    public String getAccessPath(FileResource fileResource) {
        return baseDir.resolve(fileResource.getStoragePath()).toString();
    }
    
    @Override
    public void delete(FileResource fileResource) throws IOException {
        Path filePath = baseDir.resolve(fileResource.getStoragePath()).normalize();
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
    }
    
    @Override
    public String getStorageType() {
        return "LOCAL";
    }
    
    /**
     * 构建存储路径
     * @param ext 文件扩展名
     * @return 相对存储路径
     */
    private String buildStoragePath(String ext) {
        UUID fileId = UUID.randomUUID();
        return DateTimeFormatter.ofPattern("yyyy/MM/dd")
                .format(LocalDate.now())
                + "/" + fileId + "." + ext;
    }
    
    private String calculateFileHash(InputStream inputStream) throws IOException {
        try {
            byte[] buffer = new byte[8192];
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            int numBytes;
            while ((numBytes = inputStream.read(buffer)) > 0) {
                digest.update(buffer, 0, numBytes);
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }
}