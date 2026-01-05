package com.github.im.server.mapstruct;

import com.github.im.dto.file.FileUploadResponse;
import com.github.im.dto.session.FileMeta;
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
    @Mapping(target = "duration", source = "fileResource", qualifiedByName = "mapDuration")
    FileMeta toMeta(FileResource fileResource);
    
    @Named("mapDuration")
    default Long mapDuration(FileResource fileResource) {
        // 如果文件有duration，直接使用；否则返回null
        return fileResource.getDuration();
    }

//    @Mapping(target = "fileMeta", ignore = true)
    @Mapping(target = "fileMeta", expression = "java(toMeta(fileResource))")
    FileUploadResponse toDTO(FileResource fileResource);


}