package com.github.im.server.service;

import cn.hutool.core.io.file.FileNameUtil;
import com.github.im.dto.file.FileUploadResponse;
import com.github.im.dto.session.FileMeta;
import com.github.im.server.config.FileUploadProperties;
import com.github.im.server.mapstruct.FileMapper;
import com.github.im.server.model.FileResource;
import com.github.im.server.model.MediaFileResource;
import com.github.im.server.model.enums.FileStatus;
import com.github.im.server.repository.FileResourceRepository;
import com.github.im.server.repository.MediaFileResourceRepository;
import com.github.im.server.service.storage.LocalStorageStrategy;
import com.github.im.server.service.storage.StorageStrategy;
import com.github.im.server.service.storage.StorageStrategyFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
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
    private final StorageStrategy storageStrategy;
    private final FileResourceRepository repository;
    private final MediaFileResourceRepository mediaFileResourceRepository;

    private final FileMapper fileMapper;
    private final StorageStrategyFactory storageStrategyFactory;

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
     * 获取文件信息
     * @param id 文件 id
     * @return FileResource
     * @throws FileNotFoundException 查询不到文件id 则抛出文件不存在异常
     */
    public FileResource getFileResourceById(String id ) throws FileNotFoundException {
        try{
            val uuid = UUID.fromString(id);
            return repository.findById(uuid).orElseThrow(() -> new FileNotFoundException("File not found: " + id));

        }catch (IllegalArgumentException illegalArgumentException){
            throw new FileNotFoundException("Uuid in wrong format + "+id);
        }
    }

    /**
     * 根据文件Id 获取文件的元信息数据
     * @param fileID
     * @return
     * @throws FileNotFoundException
     */

    public FileMeta getFileMeta(UUID fileID ) throws FileNotFoundException {
        FileResource fileResource = repository.findById(fileID)
                .orElseThrow(()->new FileNotFoundException("File not found : "+fileID));
        
        // 获取媒体资源信息（如果存在）
        MediaFileResource mediaResource = mediaFileResourceRepository.findByFileId(fileID);
        
        // 如果媒体资源存在且文件的duration为null，使用媒体资源的duration
        if (mediaResource != null && fileResource.getDuration() == null) {
            fileResource.setDuration(mediaResource.getDuration() != null ? mediaResource.getDuration().longValue() : null);
        }
        FileMeta meta = fileMapper.toMeta(fileResource);

        if (mediaResource != null){
            meta.setThumbnail(mediaResource.getThumbnail());
        }
        return meta;
    }



    /**
     * 存储单文件（小文件直传）
     */
    public FileUploadResponse storeFile(MultipartFile file, UUID uploaderId,Long duration) throws IOException {
        // 使用注入的存储策略存储文件
        FileResource resource = storageStrategy.store(file, uploaderId, duration);
        
        FileResource savedResource = repository.save(resource);
        
        // 如果是媒体文件，创建对应的媒体资源记录
        MediaFileResource mediaResource = createMediaResourceIfNeeded(savedResource, duration);
        
        return fileMapper.toDTO(savedResource);
    }

    /**
     * 存储分片
     * @param file  文件
     * @param fileHash 文件 hash
     * @param chunkIndex 当前分片索引
     * @param totalChunks 总分片数
     * @param clientId 客户端生成得Id
     */
    public void uploadChunk(MultipartFile file, String fileHash, int chunkIndex,
                            int totalChunks, UUID clientId) throws IOException {
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


    /***
     * 找不到的抛出 {@link FileNotFoundException } 异常
     * @param fileId 文件 id
     * @return FileResource
     */
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


    public File loadFile(FileResource fileResource) throws FileNotFoundException {
        Path filePath = baseDir.resolve(fileResource.getStoragePath()).normalize();
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new FileNotFoundException("文件不存在或不可读: " + filePath);
        }
        return filePath.toFile();
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
     * 所有的分片文件会存放在临时目录文件下
     */
    public FileUploadResponse mergeChunks(String fileHash, String originalName, UUID clientId, Long duration) throws IOException {
        // 使用存储策略的分片合并方法
        FileResource resource = storageStrategy.storeMergedFile(fileHash, originalName, clientId, duration, chunkTempDir);
        
        FileResource savedResource = repository.save(resource);
        
        // 如果是媒体文件，创建对应的媒体资源记录
        MediaFileResource mediaResource = createMediaResourceIfNeeded(savedResource, duration);
        
        return fileMapper.toDTO(savedResource);    
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
    
    /**
     * 计算文件哈希值
     */
    private String calculateFileHash(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            return DigestUtils.md5DigestAsHex(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to calculate file hash", e);
        }
    }
    
    /**
     * 根据文件信息创建媒体资源记录（如果需要）
     */
    private MediaFileResource createMediaResourceIfNeeded(FileResource fileResource, Long duration) {
        if (isMediaFile(fileResource.getExtension())) {
            MediaFileResource mediaResource = new MediaFileResource();
            mediaResource.setFile(fileResource);
            
            // 设置时长，如果传入的时长为null，则使用文件的时长
            Float mediaDuration = duration != null ? duration.floatValue() : fileResource.getDuration() != null ? fileResource.getDuration().floatValue() : 0.0f;
            mediaResource.setDuration(mediaDuration);
            
            // 如果文件资源的duration为null，但提供了duration参数，则更新文件资源
            if (fileResource.getDuration() == null && duration != null) {
                fileResource.setDuration(duration);
                repository.save(fileResource); // 更新文件资源
            }
            
            // TODO: 生成缩略图的逻辑
            // 如果是图片文件，生成缩略图
            if (isImageFile(fileResource.getExtension())) {
                // 这里可以调用图片处理服务生成缩略图
                // mediaResource.setThumbnail(generateThumbnail(fileResource));
            }
            
            return mediaFileResourceRepository.save(mediaResource);
        }
        return null;
    }

    /**
     * 检查是否为媒体文件
     */
    private boolean isMediaFile(String extension) {
        if (extension == null) return false;
        
        String ext = extension.toLowerCase();
        return ext.equals("mp4") || ext.equals("avi") || ext.equals("mov") || ext.equals("wmv") ||
               ext.equals("mp3") || ext.equals("wav") || ext.equals("flac") || ext.equals("aac") ||
               ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("gif") ||
               ext.equals("webp") || ext.equals("bmp");
    }
    
    /**
     * 检查是否为图片文件
     */
    private boolean isImageFile(String extension) {
        if (extension == null) return false;
        
        String ext = extension.toLowerCase();
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("gif") ||
               ext.equals("webp") || ext.equals("bmp");
    }
}
