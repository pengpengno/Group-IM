package com.github.im.server

import com.github.im.dto.file.FileUploadResponse
import com.github.im.server.config.FileUploadProperties
import com.github.im.server.mapstruct.FileMapper
import com.github.im.server.model.FileResource
import com.github.im.server.model.enums.FileStatus
import com.github.im.server.model.enums.StorageType
import com.github.im.server.repository.FileResourceRepository
import com.github.im.server.service.FileStorageService
import org.springframework.core.io.UrlResource
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class FileStorageServiceSpec extends Specification {

    def properties = Mock(FileUploadProperties)
    def repository = Mock(FileResourceRepository)
    def fileMapper = Mock(FileMapper)

    def fileStorageService = new FileStorageService(properties, repository, fileMapper)

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

    def "storeFile should save file and return response"() {
        given:
        def multipartFile = Mock(MultipartFile)
        def clientId = UUID.randomUUID()
        def duration = 100L
        def originalName = "test.txt"
        def contentType = "text/plain"
        def fileSize = 1024L

        def fileResource = new FileResource()
        fileResource.setId(UUID.randomUUID())
        fileResource.setOriginalName(originalName)
        fileResource.setContentType(contentType)
        fileResource.setSize(fileSize)
        fileResource.setStorageType(StorageType.LOCAL)
        fileResource.setStatus(FileStatus.NORMAL)
        fileResource.setClientId(clientId)
        fileResource.setDuration(duration)

        def fileUploadResponse = new FileUploadResponse()

        multipartFile.getOriginalFilename() >> originalName
        multipartFile.getContentType() >> contentType
        multipartFile.getSize() >> fileSize
        multipartFile.getInputStream() >> new ByteArrayInputStream("test content".getBytes())

        repository.save(_) >> fileResource
        fileMapper.toDTO(fileResource) >> fileUploadResponse

        and:
        fileStorageService.baseDir = Paths.get(System.getProperty("java.io.tmpdir"))

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
            fr.getClientId() == clientId &&
            fr.getDuration() == duration
        })
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

        cleanup:
        Files.deleteIfExists(filePath)
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

    def "mergeChunks should merge all chunks and return response"() {
        given:
        def fileHash = "abc123"
        def originalName = "merged.txt"
        def clientId = UUID.randomUUID()

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
        repository.save(_) >> fileResource
        fileMapper.toDTO(fileResource) >> fileUploadResponse

        when:
        def result = fileStorageService.mergeChunks(fileHash, originalName, clientId)

        then:
        result == fileUploadResponse

        cleanup:
        Files.deleteIfExists(sessionDir.resolve("00001.part"))
        Files.deleteIfExists(sessionDir.resolve("00002.part"))
        Files.deleteIfExists(sessionDir)
    }
}