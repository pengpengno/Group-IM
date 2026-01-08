package com.github.im.server.service;

import cn.hutool.core.io.file.FileNameUtil;
import com.github.im.dto.file.FileUploadResponse;
import com.github.im.dto.file.UploadFileRequest;
import com.github.im.dto.message.FileMeta;
import com.github.im.server.config.FileUploadProperties;
import com.github.im.server.mapstruct.FileMapper;
import com.github.im.server.model.FileResource;
import com.github.im.server.model.MediaFileResource;
import com.github.im.server.model.enums.FileStatus;
import com.github.im.server.repository.FileResourceRepository;
import com.github.im.server.repository.MediaFileResourceRepository;
import com.github.im.server.service.storage.StorageStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final FileUploadProperties properties;
    private final StorageStrategy storageStrategy;
    private final FileResourceRepository repository;
    private final MediaFileResourceRepository mediaFileResourceRepository;
    private final FileMapper fileMapper;
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
        
        FileMeta meta;
        if (mediaResource != null) {
            meta = fileMapper.toMetaWithMedia(fileResource, mediaResource);
            meta.setThumbnail(mediaResource.getThumbnail());
        } else {
            meta = fileMapper.toMeta(fileResource);
        }
        return meta;
    }



    /**
     * 存储单文件（小文件直传）
     */
    @Transactional
    public FileUploadResponse storeFile(MultipartFile file, UUID fileId,Long duration) throws IOException {
//        // 根据fileId（预分配的文件ID）查询已存在的文件记录
        FileResource existingResource = repository.findById(fileId)
            .orElseThrow(() -> new FileNotFoundException("File record not found for ID: " + fileId));
        
        // 使用现有的文件记录信息，更新文件存储
        FileResource updatedResource = storageStrategy.store(file, fileId, duration);
        
        // 保留预分配记录的关键信息
        existingResource.setUploadTime(LocalDateTime.now()); // 保留原有的上传时间
        existingResource.setStatus(FileStatus.NORMAL); // 更新状态为正常
        existingResource.setStoragePath(updatedResource.getStoragePath());
        existingResource.setStorageType(updatedResource.getStorageType());
        existingResource.setHash(updatedResource.getHash());
        existingResource.setSize(updatedResource.getSize());

        // 保存更新后的文件记录
        FileResource savedResource = repository.saveAndFlush(existingResource);
        
        // 如果是媒体文件，创建或更新对应的媒体资源记录
        MediaFileResource mediaResource = createMediaResourceIfNeeded(savedResource, duration);
        
        // 如果存在媒体资源，使用包含媒体信息的DTO方法
        if (mediaResource != null) {
            return fileMapper.toDTOMedia(savedResource, mediaResource);
        } else {
            return fileMapper.toDTO(savedResource);
        }
    }

    /**
     * 存储分片
     * @param file  文件
     * @param fileHash 文件 hash
     * @param chunkIndex 当前分片索引
     * @param totalChunks 总分片数
     * @param fileId 预分配的文件ID
     */
    public void uploadChunk(MultipartFile file, String fileHash, int chunkIndex,
                            int totalChunks, UUID fileId) throws IOException {
        Path sessionDir = chunkTempDir.resolve(fileId.toString()).normalize();
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
    public FileUploadResponse mergeChunks(String fileHash, String originalName, UUID fileId, Long duration) throws IOException {
        // 根据fileId（预分配的文件ID）查询已存在的文件记录
        FileResource existingResource = repository.findById(fileId)
            .orElseThrow(() -> new FileNotFoundException("File record not found for ID: " + fileId));
        
        // 使用存储策略的分片合并方法
        FileResource updatedResource = storageStrategy.storeMergedFile(fileHash, originalName, fileId, duration, chunkTempDir);
        
        // 保留预分配记录的关键信息
        updatedResource.setId(existingResource.getId());
        updatedResource.setUploadTime(existingResource.getUploadTime()); // 保留原有的上传时间
        updatedResource.setStatus(FileStatus.NORMAL); // 更新状态为正常
        
        // 保存更新后的文件记录
        FileResource savedResource = repository.save(updatedResource);
        
        // 如果是媒体文件，创建或更新对应的媒体资源记录
        MediaFileResource mediaResource = createMediaResourceIfNeeded(savedResource, duration);
        
        // 如果存在媒体资源，使用包含媒体信息的DTO方法
        if (mediaResource != null) {
            return fileMapper.toDTOMedia(savedResource, mediaResource);
        } else {
            return fileMapper.toDTO(savedResource);
        }    
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
     * 预创建文件占位记录，返回文件ID供后续上传使用
     * @param request 上传请求
     *                request.fileName 文件名
     *                request.size 文件大小
     *
     * @return FileUploadResponse 文件上传返回
     */
    public FileUploadResponse createFilePlaceholder(UploadFileRequest request) {
        FileResource fileResource = new FileResource();
        UUID fileId = UUID.randomUUID();
        fileResource.setId(fileId);
        
        // 设置基本信息
        Optional<MediaType> mediaType = MediaTypeFactory.getMediaType(request.getFileName());
        mediaType.ifPresent(mt -> fileResource.setContentType(mt.toString()));
        fileResource.setOriginalName(request.getFileName());
        fileResource.setSize(request.getSize());
        fileResource.setExtension(FileNameUtil.extName(request.getFileName()));
        

        
        // 设置初始状态
        fileResource.setStatus(FileStatus.UPLOADING);
        fileResource.setUploadTime(LocalDateTime.now());
        
        // 保存到数据库
        FileResource savedResource = repository.save(fileResource);
//
//        // 如果是媒体文件，创建对应的媒体资源记录
//        MediaFileResource mediaResource = createMediaResourceIfNeeded(savedResource, request.getDuration());
//
        // 如果存在媒体资源，使用包含媒体信息的DTO方法
//        if (mediaResource != null) {
//            return fileMapper.toDTOMedia(savedResource, mediaResource);
//        } else {
            return fileMapper.toDTO(savedResource);
//        }
    }
    
    /**
     * 根据文件信息创建媒体资源记录（如果需要）
     */
    private MediaFileResource createMediaResourceIfNeeded(FileResource fileResource, Long duration) {

        try{
            if (isMediaFile(fileResource.getExtension())) {
                // 先检查是否已存在对应的媒体资源记录
                var mediaResource = Optional.ofNullable(mediaFileResourceRepository.findByFileId(fileResource.getId())).orElseGet(()->{
                    MediaFileResource mediaFileResource = new MediaFileResource();
                    mediaFileResource.setFile(fileResource);
                    return  mediaFileResource;
                });
                Float mediaDuration = duration != null ? duration.floatValue() : 0.0f;
                mediaResource.setDuration(mediaDuration);
                // 如果是图片文件，生成缩略图
                if (isImageFile(fileResource.getExtension())) {
                    // 这里可以调用图片处理服务生成缩略图 TODO

                    /**
                     * 1. 创建一条缩略图的 FileResource 记录
                     * 2. 生成缩略图文件
                     * 3. 保存缩略图文件
                     * 4. 更新 FileResource  并且存入数据库
                     */
                }



                return mediaFileResourceRepository.save(mediaResource);
            }


        }catch (Exception e){
            log.error("创建媒体资源失败", e);
            throw  new RuntimeException("创建媒体资源失败");
        }
        return null;
    }

    /**
     * 检查是否为媒体文件
     */
    private boolean isMediaFile(String extension) {
        if (extension == null) return false;
        
        String ext = extension.toLowerCase();
        // 视频格式
        boolean isVideo = ext.equals("mp4") || ext.equals("avi") || ext.equals("mov") || 
                       ext.equals("wmv") || ext.equals("mkv") || ext.equals("flv") || 
                       ext.equals("webm") || ext.equals("m4v") || ext.equals("3gp");
        
        // 音频格式
        boolean isAudio = ext.equals("mp3") || ext.equals("wav") || ext.equals("flac") || 
                       ext.equals("aac") || ext.equals("ogg") || ext.equals("m4a") || 
                       ext.equals("wma") || ext.equals("opus");
        
        // 图片格式
        boolean isImage = ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || 
                       ext.equals("gif") || ext.equals("webp") || ext.equals("bmp") ||
                       ext.equals("svg") || ext.equals("tiff") || ext.equals("ico") ||
                       ext.equals("heic") || ext.equals("heif");
        
        return isVideo || isAudio || isImage;
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
