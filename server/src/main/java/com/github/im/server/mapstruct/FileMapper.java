package com.github.im.server.mapstruct;

import com.github.im.dto.file.FileUploadResponse;
import com.github.im.dto.session.FileMeta;
import com.github.im.server.model.FileResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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
//    @Mapping(target = "contentType", source = "contentType")
//    @Mapping(target = "hash", source = "hash")
    FileMeta toMeta(FileResource fileResource);

//    @Mapping(target = "fileMeta", ignore = true)
    FileUploadResponse toDTO(FileResource fileResource);


}