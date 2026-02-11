package com.github.im.server.service

import com.github.im.dto.file.FileUploadResponse
import com.github.im.dto.message.FileMeta
import com.github.im.server.config.FileUploadProperties
import com.github.im.server.mapstruct.FileMapper
import com.github.im.server.model.FileResource
import com.github.im.server.model.MediaFileResource
import com.github.im.server.model.enums.FileStatus
import com.github.im.server.model.enums.StorageType
import com.github.im.server.repository.FileResourceRepository
import com.github.im.server.repository.MediaFileResourceRepository
import com.github.im.server.service.storage.StorageStrategy
import com.github.im.server.service.storage.StorageStrategyFactory
import org.springframework.core.io.UrlResource
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths

class FileStorageServiceSpec extends Specification {

    def properties = Mock(FileUploadProperties)
    def storageStrategy = Mock(StorageStrategy)
    def repository = Mock(FileResourceRepository)
    def mediaFileResourceRepository = Mock(MediaFileResourceRepository)
    def fileMapper = Mock(FileMapper)
    def storageStrategyFactory = Mock(StorageStrategyFactory)

    def fileStorageService = new FileStorageService(
            properties, storageStrategy, repository, mediaFileResourceRepository, fileMapper, storageStrategyFactory)

    def "getFileResourceById should return file resource when exists"() {
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

    def "getFileResourceById should throw FileNotFoundException when not exists"() {
        given:
        def fileId = UUID.randomUUID()
        repository.findById(fileId) >> Optional.empty()

        when:
        fileStorageService.getFileResourceById(fileId.toString())

        then:
        thrown(FileNotFoundException)
    }

    def "getFileResourceById should throw FileNotFoundException when invalid UUID format"() {
        given:
        def invalidId = "invalid-uuid"

        when:
        fileStorageService.getFileResourceById(invalidId)

        then:
        thrown(FileNotFoundException)
    }

    def "getFileMeta should return file meta with media info when media resource exists"() {
        given:
        def fileId = UUID.randomUUID()
        def fileResource = new FileResource()
        fileResource.setId(fileId)
        fileResource.setOriginalName("test.mp4")
        fileResource.setSize(1024L)
        fileResource.setContentType("video/mp4")

        def mediaResource = new MediaFileResource()
        mediaResource.setDuration(120.5f)
        mediaResource.setThumbnail("thumbnail-url")

        def fileMeta = new FileMeta()
        fileMeta.setFileId(fileId.toString())
        fileMeta.setFilename("test.mp4")
        fileMeta.setFileSize(1024L)
        fileMeta.setDuration(121L) // duration should come from media resource

        repository.findById(fileId) >> Optional.of(fileResource)
        mediaFileResourceRepository.findByFileId(fileId) >> mediaResource
        fileMapper.toMetaWithMedia(fileResource, mediaResource) >> fileMeta

        when:
        def result = fileStorageService.getFileMeta(fileId)

        then:
        result.getFileId() == fileId.toString()
        result.getDuration() == 121L // from media resource
        result.getThumbnail() == "thumbnail-url"
    }

    def "getFileMeta should return file meta without media info when media resource does not exist"() {
        given:
        def fileId = UUID.randomUUID()
        def fileResource = new FileResource()
        fileResource.setId(fileId)
        fileResource.setOriginalName("test.txt")
        fileResource.setSize(1024L)
        fileResource.setContentType("text/plain")

        def fileMeta = new FileMeta()
        fileMeta.setFileId(fileId.toString())
        fileMeta.setFilename("test.txt")
        fileMeta.setFileSize(1024L)

        repository.findById(fileId) >> Optional.of(fileResource)
        mediaFileResourceRepository.findByFileId(fileId) >> null
        fileMapper.toMeta(fileResource) >> fileMeta

        when:
        def result = fileStorageService.getFileMeta(fileId)

        then:
        result.getFileId() == fileId.toString()
        result.getFilename() == "test.txt"
        result.getFileSize() == 1024L
        result.getDuration() == null
    }

    def "storeFile should save file and return response"() {
        given:
        def multipartFile = Mock(MultipartFile)
        def clientId = UUID.randomUUID()
        def duration = 100L
        def originalName = "test.mp4"
        def contentType = "video/mp4"
        def fileSize = 1024L
        def fileHash = "abc123"
        def storagePath = "2025/04/25/test.mp4"

        def fileResource = new FileResource()
        fileResource.setId(UUID.randomUUID())
        fileResource.setOriginalName(originalName)
        fileResource.setContentType(contentType)
        fileResource.setSize(fileSize)
        fileResource.setStorageType(StorageType.LOCAL)
        fileResource.setStatus(FileStatus.NORMAL)
        fileResource.setClientId(clientId)
        fileResource.setHash(fileHash)
        fileResource.setStoragePath(storagePath)

        def mediaResource = new MediaFileResource()
        mediaResource.setDuration(100.0f)

        def fileUploadResponse = new FileUploadResponse()
        def mappedFileMeta = new FileMeta()

        multipartFile.getOriginalFilename() >> originalName
        multipartFile.getContentType() >> contentType
        multipartFile.getSize() >> fileSize

        storageStrategy.store(multipartFile, clientId, duration) >> fileResource
        repository.save(fileResource) >> fileResource
        mediaFileResourceRepository.save(mediaResource) >> mediaResource
        fileMapper.toDTOMedia(fileResource, mediaResource) >> fileUploadResponse
        fileMapper.toMetaWithMedia(fileResource, mediaResource) >> mappedFileMeta

        when:
        def result = fileStorageService.storeFile(multipartFile, clientId, duration)

        then:
        result == fileUploadResponse
        1 * repository.save({ FileResource fr ->
            fr.getOriginalName() == originalName &&
            fr.getContentType() == contentType &&
            fr.getSize() == fileSize &&
            fr.getStorageType() == StorageType.LOCAL &&
            fr.getStatus() == FileStatus.NORMAL &&
            fr.getClientId() == clientId
        })
        1 * mediaFileResourceRepository.findByFileId(fileResource.getId())
    }

    def "storeFile should return simple response when not media file"() {
        given:
        def multipartFile = Mock(MultipartFile)
        def clientId = UUID.randomUUID()
        def duration = 100L
        def originalName = "test.txt"
        def contentType = "text/plain"
        def fileSize = 1024L
        def fileHash = "abc123"
        def storagePath = "2025/04/25/test.txt"

        def fileResource = new FileResource()
        fileResource.setId(UUID.randomUUID())
        fileResource.setOriginalName(originalName)
        fileResource.setContentType(contentType)
        fileResource.setSize(fileSize)
        fileResource.setStorageType(StorageType.LOCAL)
        fileResource.setStatus(FileStatus.NORMAL)
        fileResource.setClientId(clientId)
        fileResource.setHash(fileHash)
        fileResource.setStoragePath(storagePath)

        def fileUploadResponse = new FileUploadResponse()

        multipartFile.getOriginalFilename() >> originalName
        multipartFile.getContentType() >> contentType
        multipartFile.getSize() >> fileSize

        storageStrategy.store(multipartFile, clientId, duration) >> fileResource
        repository.save(fileResource) >> fileResource
        fileMapper.toDTO(fileResource) >> fileUploadResponse

        when:
        def result = fileStorageService.storeFile(multipartFile, clientId, duration)

        then:
        result == fileUploadResponse
        1 * repository.save({ FileResource fr ->
            fr.getOriginalName() == originalName &&
            fr.getContentType() == contentType &&
            fr.getSize() == fileSize &&
            fr.getStorageType() == StorageType.LOCAL &&
            fr.getStatus() == FileStatus.NORMAL &&
            fr.getClientId() == clientId
        })
        0 * mediaFileResourceRepository.save(_)
    }

    def "uploadChunk should save chunk file"() {
        given:
        def multipartFile = Mock(MultipartFile)
        def fileHash = "abc123"
        def chunkIndex = 1
        def totalChunks = 5
        def clientId = UUID.randomUUID()

        and:
        fileStorageService.chunkTempDir = Paths.get(System.getProperty("java.io.tmpdir"))

        when:
        fileStorageService.uploadChunk(multipartFile, fileHash, chunkIndex, totalChunks, clientId)

        then:
        1 * multipartFile.transferTo(_)
    }

    def "getUploadedChunks should return list of uploaded chunks"() {
        given:
        def fileHash = "abc123"
        fileStorageService.chunkTempDir = Paths.get(System.getProperty("java.io.tmpdir"))

        and:
        def sessionDir = fileStorageService.chunkTempDir.resolve(fileHash)
        Files.createDirectories(sessionDir)
        Files.createFile(sessionDir.resolve("00001.part"))
        Files.createFile(sessionDir.resolve("00002.part"))

        when:
        def result = fileStorageService.getUploadedChunks(fileHash)

        then:
        result == [1, 2]

        cleanup:
        Files.deleteIfExists(sessionDir.resolve("00001.part"))
        Files.deleteIfExists(sessionDir.resolve("00002.part"))
        Files.deleteIfExists(sessionDir)
    }

    def "getFile should return file resource when exists"() {
        given:
        def fileId = UUID.randomUUID()
        def fileResource = new FileResource()
        fileResource.setId(fileId)

        repository.findById(fileId) >> Optional.of(fileResource)

        when:
        def result = fileStorageService.getFile(fileId)

        then:
        result == fileResource
    }

    def "getFile should throw FileNotFoundException when not exists"() {
        given:
        def fileId = UUID.randomUUID()
        repository.findById(fileId) >> Optional.empty()

        when:
        fileStorageService.getFile(fileId)

        then:
        thrown(FileNotFoundException)
    }

    def "loadFileAsResource should return UrlResource when file exists"() {
        given:
        def fileResource = new FileResource()
        fileResource.setStoragePath("test.txt")

        and:
        fileStorageService.baseDir = Paths.get(System.getProperty("java.io.tmpdir"))
        def filePath = fileStorageService.baseDir.resolve("test.txt")
        Files.createFile(filePath)

        when:
        def result = fileStorageService.loadFileAsResource(fileResource)

        then:
        result instanceof UrlResource

//        cleanup:
//        Files.deleteIfExists(filePath)
    }

    def "loadFileAsResource should throw FileNotFoundException when file not exists"() {
        given:
        def fileResource = new FileResource()
        fileResource.setStoragePath("nonexistent.txt")

        and:
        fileStorageService.baseDir = Paths.get(System.getProperty("java.io.tmpdir"))

        when:
        fileStorageService.loadFileAsResource(fileResource)

        then:
        thrown(FileNotFoundException)
    }

    def "mergeChunks should merge all chunks and return response with media info"() {
        given:
        def fileHash = "abc123"
        def originalName = "merged.mp4"
        def clientId = UUID.randomUUID()
        def duration = 150L

        def fileResource = new FileResource()
        fileResource.setId(UUID.randomUUID())
        fileResource.setOriginalName(originalName)
        fileResource.setSize(2048L)
        fileResource.setStorageType(StorageType.LOCAL)
        fileResource.setStatus(FileStatus.NORMAL)
        fileResource.setClientId(clientId)

        def mediaResource = new MediaFileResource()
        mediaResource.setDuration(150.0f)

        def fileUploadResponse = new FileUploadResponse()

        and:
        fileStorageService.baseDir = Paths.get(System.getProperty("java.io.tmpdir"))
        fileStorageService.chunkTempDir = Paths.get(System.getProperty("java.io.tmpdir"))

        and:
        def sessionDir = fileStorageService.chunkTempDir.resolve(fileHash)
        Files.createDirectories(sessionDir)
        Files.createFile(sessionDir.resolve("00001.part"))
        Files.createFile(sessionDir.resolve("00002.part"))

        and:
        storageStrategy.storeMergedFile(fileHash, originalName, clientId, duration, _) >> fileResource
        repository.save(fileResource) >> fileResource
        mediaFileResourceRepository.save(mediaResource) >> mediaResource
        fileMapper.toDTOMedia(fileResource, mediaResource) >> fileUploadResponse

        when:
        def result = fileStorageService.mergeChunks(fileHash, originalName, clientId, duration)

        then:
        result == fileUploadResponse

        cleanup:
        Files.deleteIfExists(sessionDir.resolve("00001.part"))
        Files.deleteIfExists(sessionDir.resolve("00002.part"))
        Files.deleteIfExists(sessionDir)
    }

    def "mergeChunks should merge all chunks and return simple response for non-media file"() {
        given:
        def fileHash = "abc123"
        def originalName = "merged.txt"
        def clientId = UUID.randomUUID()
        def duration = null

        def fileResource = new FileResource()
        fileResource.setId(UUID.randomUUID())
        fileResource.setOriginalName(originalName)
        fileResource.setSize(2048L)
        fileResource.setStorageType(StorageType.LOCAL)
        fileResource.setStatus(FileStatus.NORMAL)
        fileResource.setClientId(clientId)

        def fileUploadResponse = new FileUploadResponse()

        and:
        fileStorageService.baseDir = Paths.get(System.getProperty("java.io.tmpdir"))
        fileStorageService.chunkTempDir = Paths.get(System.getProperty("java.io.tmpdir"))

        and:
        def sessionDir = fileStorageService.chunkTempDir.resolve(fileHash)
        Files.createDirectories(sessionDir)
        Files.createFile(sessionDir.resolve("00001.part"))
        Files.createFile(sessionDir.resolve("00002.part"))

        and:
        storageStrategy.storeMergedFile(fileHash, originalName, clientId, duration, _) >> fileResource
        repository.save(fileResource) >> fileResource
        fileMapper.toDTO(fileResource) >> fileUploadResponse

        when:
        def result = fileStorageService.mergeChunks(fileHash, originalName, clientId, duration)

        then:
        result == fileUploadResponse

        cleanup:
        Files.deleteIfExists(sessionDir.resolve("00001.part"))
        Files.deleteIfExists(sessionDir.resolve("00002.part"))
        Files.deleteIfExists(sessionDir)
    }
}