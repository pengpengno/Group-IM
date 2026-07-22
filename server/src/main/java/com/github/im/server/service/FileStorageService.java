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

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final SystemConfigService systemConfigService;
    private final StorageStrategy storageStrategy;
    private final FileResourceRepository repository;
    private final MediaFileResourceRepository mediaFileResourceRepository;
    private final FileMapper fileMapper;
    private Path baseDir;
    private Path chunkTempDir;
    private Path previewCacheDir;

    @PostConstruct
    public void init() throws IOException {
        // 解析 basePath
        baseDir = resolvePath(properties.getBasePath());
        chunkTempDir = resolvePath(properties.getChunkTempPath());

        // 创建根目录
        Files.createDirectories(baseDir);
        Files.createDirectories(chunkTempDir);
        previewCacheDir = baseDir.resolve(".variants").normalize();
        Files.createDirectories(previewCacheDir);

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
     * @param fileID 文件的 UUID
     * @return 返回文件元数据信息
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

    public File loadPreviewFile(UUID fileId, Integer width, Integer quality) throws IOException {
        FileResource fileResource = getFile(fileId);
        if (fileResource.getStatus() != FileStatus.NORMAL) {
            throw new FileNotFoundException("Preview not available: " + fileId);
        }

        MediaFileResource mediaResource = mediaFileResourceRepository.findByFileId(fileId);
        if (mediaResource != null && mediaResource.getThumbnail() != null && !mediaResource.getThumbnail().isBlank()) {
            try {
                FileResource thumbnailResource = getFile(UUID.fromString(mediaResource.getThumbnail()));
                if (thumbnailResource.getStatus() == FileStatus.NORMAL) {
                    return loadFile(thumbnailResource);
                }
            } catch (Exception ex) {
                log.warn("Failed to use thumbnail {} for file {}", mediaResource.getThumbnail(), fileId, ex);
            }
        }

        if (!isPreviewableImage(fileResource)) {
            return loadFile(fileResource);
        }

        int previewWidth = sanitizePreviewWidth(width);
        int previewQuality = sanitizePreviewQuality(quality);
        return buildOrReuseImagePreview(fileResource, previewWidth, previewQuality);
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
                mediaResource.setThumbnail(ensureMediaThumbnail(fileResource, mediaResource));
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

    private boolean isPreviewableImage(FileResource fileResource) {
        if (fileResource == null) {
            return false;
        }
        if (fileResource.getContentType() != null && fileResource.getContentType().startsWith("image/")) {
            return !fileResource.getContentType().equalsIgnoreCase("image/svg+xml");
        }
        return isImageFile(fileResource.getExtension());
    }

    private int sanitizePreviewWidth(Integer width) {
        SystemConfigService.MediaRuntimePolicy preview = systemConfigService.getMediaRuntimePolicy();
        if (width == null) {
            return preview.getPreviewDefaultWidth();
        }
        return Math.max(preview.getPreviewMinWidth(), Math.min(width, preview.getPreviewMaxWidth()));
    }

    private int sanitizePreviewQuality(Integer quality) {
        SystemConfigService.MediaRuntimePolicy preview = systemConfigService.getMediaRuntimePolicy();
        if (quality == null) {
            return preview.getPreviewDefaultQuality();
        }
        return Math.max(preview.getPreviewMinQuality(), Math.min(quality, preview.getPreviewMaxQuality()));
    }

    private File buildOrReuseImagePreview(FileResource fileResource, int previewWidth, int previewQuality) throws IOException {
        File originalFile = loadFile(fileResource);
        BufferedImage sourceImage = ImageIO.read(originalFile);
        if (sourceImage == null || sourceImage.getWidth() <= previewWidth) {
            return originalFile;
        }

        Path fileVariantDir = previewCacheDir.resolve(fileResource.getId().toString()).normalize();
        Files.createDirectories(fileVariantDir);

        String format = resolvePreviewFormat(fileResource.getContentType(), fileResource.getExtension());
        Path previewPath = fileVariantDir.resolve("w" + previewWidth + "-q" + previewQuality + "." + format);
        if (Files.exists(previewPath) && Files.isReadable(previewPath)) {
            return previewPath.toFile();
        }

        int targetHeight = Math.max(1, (int) Math.round((double) sourceImage.getHeight() * previewWidth / sourceImage.getWidth()));
        int imageType = "png".equals(format) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage resizedImage = new BufferedImage(previewWidth, targetHeight, imageType);
        Graphics2D graphics = resizedImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (imageType == BufferedImage.TYPE_INT_RGB) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, previewWidth, targetHeight);
            }
            graphics.drawImage(sourceImage, 0, 0, previewWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }

        writePreviewImage(resizedImage, format, previewPath, previewQuality);
        return previewPath.toFile();
    }

    private void writePreviewImage(BufferedImage image, String format, Path previewPath, int previewQuality) throws IOException {
        if ("jpg".equals(format)) {
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            try (OutputStream fileOutputStream = Files.newOutputStream(previewPath);
                 ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(fileOutputStream)) {
                writer.setOutput(imageOutputStream);
                ImageWriteParam writeParam = writer.getDefaultWriteParam();
                if (writeParam.canWriteCompressed()) {
                    writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    writeParam.setCompressionQuality(previewQuality / 100.0f);
                }
                writer.write(null, new IIOImage(image, null, null), writeParam);
            } finally {
                writer.dispose();
            }
            return;
        }

        try (OutputStream fileOutputStream = Files.newOutputStream(previewPath)) {
            ImageIO.write(image, format, fileOutputStream);
        }
    }

    private String resolvePreviewFormat(String contentType, String extension) {
        if (contentType != null && contentType.equalsIgnoreCase("image/png")) {
            return "png";
        }
        if (extension != null && extension.equalsIgnoreCase("png")) {
            return "png";
        }
        return "jpg";
    }

    private boolean isVideoFile(String extension) {
        if (extension == null) return false;
        String ext = extension.toLowerCase();
        return ext.equals("mp4") || ext.equals("avi") || ext.equals("mov") ||
            ext.equals("wmv") || ext.equals("mkv") || ext.equals("flv") ||
            ext.equals("webm") || ext.equals("m4v") || ext.equals("3gp");
    }

    private boolean isAudioFile(String extension) {
        if (extension == null) return false;
        String ext = extension.toLowerCase();
        return ext.equals("mp3") || ext.equals("wav") || ext.equals("flac") ||
            ext.equals("aac") || ext.equals("ogg") || ext.equals("m4a") ||
            ext.equals("wma") || ext.equals("opus");
    }

    private String ensureMediaThumbnail(FileResource sourceFile, MediaFileResource mediaResource) throws IOException {
        SystemConfigService.MediaRuntimePolicy mediaPolicy = systemConfigService.getMediaRuntimePolicy();
        if (!mediaPolicy.isThumbnailEnabled()) {
            return mediaResource.getThumbnail();
        }
        String existingThumbnailId = mediaResource.getThumbnail();
        if (existingThumbnailId != null && !existingThumbnailId.isBlank()) {
            try {
                FileResource existingThumbnail = getFile(UUID.fromString(existingThumbnailId));
                if (existingThumbnail.getStatus() == FileStatus.NORMAL) {
                    File thumbnailFile = loadFile(existingThumbnail);
                    if (thumbnailFile.exists() && thumbnailFile.isFile()) {
                        return existingThumbnail.getId().toString();
                    }
                }
            } catch (Exception ex) {
                log.warn("Thumbnail {} for file {} is invalid, regenerating",
                    existingThumbnailId, sourceFile.getId(), ex);
            }
        }

        if (!isVideoFile(sourceFile.getExtension()) && !isAudioFile(sourceFile.getExtension())) {
            return existingThumbnailId;
        }

        BufferedImage thumbnailImage = createMediaPosterImage(sourceFile);
        FileResource thumbnailResource = persistDerivedImageResource(sourceFile, thumbnailImage, "jpg", "image/jpeg");
        return thumbnailResource.getId().toString();
    }

    private BufferedImage createMediaPosterImage(FileResource sourceFile) {
        boolean video = isVideoFile(sourceFile.getExtension());
        SystemConfigService.MediaRuntimePolicy mediaPolicy = systemConfigService.getMediaRuntimePolicy();
        int width = Math.max(240, mediaPolicy.getThumbnailWidth());
        int height = Math.max(160, mediaPolicy.getThumbnailHeight());
        BufferedImage poster = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = poster.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            GradientPaint background = new GradientPaint(
                0, 0, video ? new Color(33, 45, 70) : new Color(46, 39, 62),
                width, height, video ? new Color(12, 16, 27) : new Color(20, 18, 30)
            );
            graphics.setPaint(background);
            graphics.fillRect(0, 0, width, height);

            graphics.setColor(new Color(255, 255, 255, 30));
            graphics.fillRoundRect(36, 36, width - 72, height - 72, 32, 32);

            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font("SansSerif", Font.BOLD, 64));
            graphics.drawString(video ? "\u25B6" : "\u266B", 72, 138);

            graphics.setFont(new Font("SansSerif", Font.BOLD, 28));
            graphics.drawString(video ? "VIDEO" : "AUDIO", 72, 206);

            graphics.setFont(new Font("SansSerif", Font.PLAIN, 24));
            graphics.drawString(compactDisplayText(sourceFile.getOriginalName(), 34), 72, 258);

            graphics.setColor(new Color(255, 255, 255, 220));
            graphics.setFont(new Font("SansSerif", Font.PLAIN, 18));
            graphics.drawString(buildPosterMetaLine(sourceFile), 72, 296);
        } finally {
            graphics.dispose();
        }
        return poster;
    }

    private String buildPosterMetaLine(FileResource sourceFile) {
        String extension = Optional.ofNullable(sourceFile.getExtension()).orElse("").toUpperCase();
        String sizeLabel = humanReadableSize(sourceFile.getSize());
        if (extension.isBlank()) {
            return sizeLabel;
        }
        return extension + "  " + sizeLabel;
    }

    private String compactDisplayText(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "media";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 1)) + "\u2026";
    }

    private String humanReadableSize(long size) {
        if (size < 1024) {
            return size + " B";
        }
        double value = size;
        String[] units = {"KB", "MB", "GB"};
        int unitIndex = -1;
        while (value >= 1024 && unitIndex + 1 < units.length) {
            value = value / 1024.0;
            unitIndex++;
        }
        return String.format("%.1f %s", value, units[Math.max(0, unitIndex)]);
    }

    private FileResource persistDerivedImageResource(FileResource sourceFile,
                                                     BufferedImage image,
                                                     String format,
                                                     String contentType) throws IOException {
        UUID derivedId = UUID.randomUUID();
        String extension = format.equalsIgnoreCase("jpg") ? "jpg" : format.toLowerCase();
        String relativePath = buildDerivedStoragePath(derivedId, extension);
        Path targetPath = baseDir.resolve(relativePath).normalize();
        Files.createDirectories(targetPath.getParent());
        SystemConfigService.MediaRuntimePolicy mediaPolicy = systemConfigService.getMediaRuntimePolicy();
        writePreviewImage(image, extension, targetPath, mediaPolicy.getThumbnailQuality());

        String hash;
        try (InputStream inputStream = Files.newInputStream(targetPath)) {
            hash = DigestUtils.md5DigestAsHex(inputStream);
        }

        FileResource thumbnailResource = new FileResource();
        thumbnailResource.setId(derivedId);
        thumbnailResource.setOriginalName(
            Optional.ofNullable(sourceFile.getOriginalName()).orElse("media") + ".preview." + extension
        );
        thumbnailResource.setExtension(extension);
        thumbnailResource.setContentType(contentType);
        thumbnailResource.setSize(Files.size(targetPath));
        thumbnailResource.setStoragePath(relativePath);
        thumbnailResource.setStorageType(com.github.im.server.model.enums.StorageType.LOCAL);
        thumbnailResource.setHash(hash);
        thumbnailResource.setUploadTime(LocalDateTime.now());
        thumbnailResource.setStatus(FileStatus.NORMAL);
        thumbnailResource.setRemark("derived-preview:" + sourceFile.getId());
        return repository.save(thumbnailResource);
    }

    private String buildDerivedStoragePath(UUID derivedId, String extension) {
        return DateTimeFormatter.ofPattern("yyyy/MM/dd")
            .format(LocalDate.now()) + "/" + derivedId + "." + extension;
    }
}
