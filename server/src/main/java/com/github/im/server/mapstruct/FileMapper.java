package com.github.im.server.mapstruct;

import com.github.im.dto.file.FileUploadResponse;
import com.github.im.dto.message.FileMeta;
import com.github.im.server.model.FileResource;
import com.github.im.server.model.MediaFileResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/5/7
 */
@Mapper(componentModel = "spring")
public interface FileMapper {


    @Mapping(target = "filename", source = "originalName")
    @Mapping(target = "fileSize", source = "size")
    @Mapping(target = "fileId", source = "id")
    @Mapping(target = "fileStatus", source = "status")
    // duration 字段已从 FileResource 中移除，现在存储在 MediaFileResource 中
    FileMeta toMeta(FileResource fileResource);
    
    @Mapping(target = "fileId", source = "fileResource.id")
    @Mapping(target = "filename", source = "fileResource.originalName")
    @Mapping(target = "fileSize", source = "fileResource.size")
    @Mapping(target = "duration", source = "mediaFileResource", qualifiedByName = "mapDurationFromMedia")
    @Mapping(target = "fileStatus", source = "fileResource.status")
    FileMeta toMetaWithMedia(FileResource fileResource, MediaFileResource mediaFileResource);
    
    @Named("mapDurationFromMedia")
    default Long mapDurationFromMedia(MediaFileResource mediaFileResource) {
        // 从媒体资源中获取时长
        return mediaFileResource != null && mediaFileResource.getDuration() != null ? 
               mediaFileResource.getDuration().longValue() : null;
    }

    @Mapping(target = "fileMeta", expression = "java(toMeta(fileResource))")
    @Mapping(target = "fileStatus", source = "fileResource.status")
    @Mapping(target = "id", source = "fileResource.id")
    FileUploadResponse toDTO(FileResource fileResource);
    
    @Mapping(target = "id", source = "fileResource.id")
    @Mapping(target = "fileMeta", expression = "java(toMetaWithMedia(fileResource, mediaFileResource))")
    @Mapping(target = "fileStatus", source = "fileResource.status")
    FileUploadResponse toDTOMedia(FileResource fileResource, MediaFileResource mediaFileResource);


}