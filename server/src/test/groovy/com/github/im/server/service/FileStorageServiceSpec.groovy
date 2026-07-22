package com.github.im.server.service

import com.github.im.dto.file.FileUploadResponse
import com.github.im.dto.file.UploadFileRequest
import com.github.im.dto.message.FileMeta
import com.github.im.server.config.FileUploadProperties
import com.github.im.server.mapstruct.FileMapper
import com.github.im.server.model.FileResource
import com.github.im.server.model.MediaFileResource
import com.github.im.server.model.enums.FileStatus
import com.github.im.server.repository.FileResourceRepository
import com.github.im.server.repository.MediaFileResourceRepository
import com.github.im.server.service.storage.StorageStrategy
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

import java.nio.file.FileAlreadyExistsException
import java.util.UUID

class FileStorageServiceSpec extends Specification {

    def properties = Mock(FileUploadProperties)
    def storageStrategy = Mock(StorageStrategy)
    def repository = Mock(FileResourceRepository)
    def mediaFileResourceRepository = Mock(MediaFileResourceRepository)
    def fileMapper = Mock(FileMapper)

    def fileStorageService = new FileStorageService(
            properties,
            storageStrategy,
            repository,
            mediaFileResourceRepository,
            fileMapper
    )

    def "getFileResourceById should return file resource when it exists"() {
        given:
        def fileId = UUID.randomUUID()
        def fileResource = new FileResource()
        fileResource.setId(fileId)

        repository.findById(fileId) >> Optional.of(fileResource)

        when:
        def result = fileStorageService.getFileResourceById(fileId.toString())

        then:
        result == fileResource
    }

    def "getFileResourceById should throw when uuid format is invalid"() {
        when:
        fileStorageService.getFileResourceById("invalid-uuid")

        then:
        thrown(FileNotFoundException)
    }

    def "getFileMeta should return uploading metadata for pending attachments"() {
        given:
        def fileId = UUID.randomUUID()
        def fileResource = new FileResource()
        fileResource.setId(fileId)
        fileResource.setOriginalName("pending-image.heic")
        fileResource.setSize(4096L)
        fileResource.setContentType("image/heic")
        fileResource.setStatus(FileStatus.UPLOADING)

        def fileMeta = FileMeta.builder()
                .fileId(fileId.toString())
                .filename("pending-image.heic")
                .fileSize(4096L)
                .contentType("image/heic")
                .fileStatus(FileStatus.UPLOADING.name())
                .build()

        repository.findById(fileId) >> Optional.of(fileResource)
        mediaFileResourceRepository.findByFileId(fileId) >> null
        fileMapper.toMeta(fileResource) >> fileMeta

        when:
        def result = fileStorageService.getFileMeta(fileId)

        then:
        result.fileId == fileId.toString()
        result.filename == "pending-image.heic"
        result.fileStatus == FileStatus.UPLOADING.name()
    }

    def "getFileMeta should include media info when media resource exists"() {
        given:
        def fileId = UUID.randomUUID()
        def fileResource = new FileResource()
        fileResource.setId(fileId)
        fileResource.setOriginalName("clip.mp4")
        fileResource.setSize(1024L)
        fileResource.setContentType("video/mp4")
        fileResource.setStatus(FileStatus.NORMAL)

        def mediaResource = new MediaFileResource()
        mediaResource.setDuration(120.5f)
        mediaResource.setThumbnail("thumb-id")
        mediaResource.setFile(fileResource)

        def fileMeta = FileMeta.builder()
                .fileId(fileId.toString())
                .filename("clip.mp4")
                .fileSize(1024L)
                .contentType("video/mp4")
                .duration(120L)
                .thumbnail("thumb-id")
                .fileStatus(FileStatus.NORMAL.name())
                .build()

        repository.findById(fileId) >> Optional.of(fileResource)
        mediaFileResourceRepository.findByFileId(fileId) >> mediaResource
        fileMapper.toMetaWithMedia(fileResource, mediaResource) >> fileMeta

        when:
        def result = fileStorageService.getFileMeta(fileId)

        then:
        result.duration == 120L
        result.thumbnail == "thumb-id"
        result.fileStatus == FileStatus.NORMAL.name()
    }

    def "createFilePlaceholder should persist uploading placeholder and expose status to client"() {
        given:
        def request = new UploadFileRequest()
        request.setFileName("pending-image.heic")
        request.setSize(4096L)

        def savedResource = new FileResource()
        savedResource.setId(UUID.randomUUID())
        savedResource.setOriginalName("pending-image.heic")
        savedResource.setSize(4096L)
        savedResource.setExtension("heic")
        savedResource.setContentType("image/heic")
        savedResource.setStatus(FileStatus.UPLOADING)

        def response = new FileUploadResponse()
        response.setId(savedResource.getId())
        response.setFileStatus(FileStatus.UPLOADING.name())
        response.setFileMeta(FileMeta.builder()
                .fileId(savedResource.getId().toString())
                .filename("pending-image.heic")
                .fileSize(4096L)
                .contentType("image/heic")
                .fileStatus(FileStatus.UPLOADING.name())
                .build())

        when:
        def result = fileStorageService.createFilePlaceholder(request)

        then:
        result.id == savedResource.getId()
        result.fileStatus == FileStatus.UPLOADING.name()
        result.fileMeta.fileStatus == FileStatus.UPLOADING.name()
        1 * repository.save({ FileResource file ->
            file.originalName == "pending-image.heic" &&
                    file.size == 4096L &&
                    file.extension == "heic" &&
                    file.status == FileStatus.UPLOADING
        }) >> savedResource
        1 * fileMapper.toDTO(savedResource) >> response
    }

    def "loadPreviewFile should reject attachments that are not ready"() {
        given:
        def fileId = UUID.randomUUID()
        def fileResource = new FileResource()
        fileResource.setId(fileId)
        fileResource.setStatus(FileStatus.UPLOADING)

        repository.findById(fileId) >> Optional.of(fileResource)

        when:
        fileStorageService.loadPreviewFile(fileId, 480, 75)

        then:
        def ex = thrown(FileNotFoundException)
        ex.message.contains("Preview not available")
    }
}
