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

import org.springframework.core.io.UrlResource
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths
import java.io.FileNotFoundException
import java.time.LocalDateTime
import java.util.UUID
import java.time.LocalDateTime

class FileStorageServiceSpec extends Specification {

    def properties = Mock(FileUploadProperties)
    def storageStrategy = Mock(StorageStrategy)
    def repository = Mock(FileResourceRepository)
    def mediaFileResourceRepository = Mock(MediaFileResourceRepository)
    def fileMapper = Mock(FileMapper)
    def fileStorageService = new FileStorageService(
            properties, storageStrategy, repository, mediaFileResourceRepository, fileMapper)

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
        def e = thrown(FileNotFoundException)
        e.message.contains("Uuid in wrong format")
    }

    def "getFileMeta should return file meta with media info when media resource exists"() {
        given:
        def fileId = UUID.randomUUID()
        def fileResource = new FileResource()
        fileResource.setId(fileId)
        fileResource.setOriginalName("test.mp4")
        fileResource.setSize(1024L)
        fileResource.setContentType("video/mp4")
        fileResource.setStatus(FileStatus.NORMAL)

        def mediaResource = new MediaFileResource()
        mediaResource.setId(1L)
        mediaResource.setDuration(120.5f)
        mediaResource.setThumbnail("thumbnail-url")
        mediaResource.setFile(fileResource)

        def fileMeta = new FileMeta()
        fileMeta.setFileId(fileId.toString())
        fileMeta.setFilename("test.mp4")
        fileMeta.setFileSize(1024L)
        fileMeta.setDuration(121L) // duration should come from media resource
        fileMeta.setThumbnail("thumbnail-url")

        repository.findById(fileId) >> Optional.of(fileResource)
        mediaFileResourceRepository.findByFileId(fileId) >> mediaResource
        fileMapper.toMetaWithMedia(fileResource, mediaResource) >> fileMeta

        when:
        def result = fileStorageService.getFileMeta(fileId)

        then:
        result.getFileId() == fileId.toString()
        result.getDuration() == 121L // from media resource
        result.getThumbnail() == "thumbnail-url"
        result.getFilename() == "test.mp4"
        result.getFileSize() == 1024L
        1 * repository.findById(fileId)
        1 * mediaFileResourceRepository.findByFileId(fileId)
        1 * fileMapper.toMetaWithMedia(fileResource, mediaResource)
    }

    def "getFileMeta should return file meta without media info when media resource does not exist"() {
        given:
        def fileId = UUID.randomUUID()
        def fileResource = new FileResource()
        fileResource.setId(fileId)
        fileResource.setOriginalName("test.txt")
        fileResource.setSize(1024L)
        fileResource.setContentType("text/plain")
        fileResource.setStatus(FileStatus.NORMAL)

        def fileMeta = new FileMeta()
        fileMeta.setFileId(fileId.toString())
        fileMeta.setFilename("test.txt")
        fileMeta.setFileSize(1024L)
        fileMeta.setDuration(null)

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
        result.getThumbnail() == null
        1 * repository.findById(fileId)
        1 * mediaFileResourceRepository.findByFileId(fileId)
        1 * fileMapper.toMeta(fileResource)
    }

    def "storeFile should save file and return response"() {
        given:
        def multipartFile = Mock(MultipartFile)
        def fileId = UUID.randomUUID()
        def duration = 100L
        def originalName = "test.mp4"
        def contentType = "video/mp4"
        def fileSize = 1024L
        def fileHash = "abc123"
        def storagePath = "2025/04/25/" + UUID.randomUUID().toString() + ".mp4"

        def existingResource = new FileResource()
        existingResource.setId(fileId)
        existingResource.setOriginalName(originalName)
        existingResource.setContentType(contentType)
        existingResource.setSize(fileSize)
        existingResource.setStorageType(StorageType.LOCAL)
        existingResource.setStatus(FileStatus.UPLOADING)
        existingResource.setUploadTime(LocalDateTime.now())
        existingResource.setExtension("mp4")

        def updatedResource = new FileResource()
        updatedResource.setId(fileId)
        updatedResource.setOriginalName(originalName)
        updatedResource.setContentType(contentType)
        updatedResource.setSize(fileSize)
        updatedResource.setStorageType(StorageType.LOCAL)
        updatedResource.setStatus(FileStatus.NORMAL)
        updatedResource.setStoragePath(storagePath)
        updatedResource.setHash(fileHash)
        updatedResource.setExtension("mp4")

        def mediaResource = new MediaFileResource()
        mediaResource.setDuration(100.0f)
        mediaResource.setFile(existingResource)

        def fileUploadResponse = new FileUploadResponse()
        fileUploadResponse.setId(fileId)
        def mappedFileMeta = new FileMeta()
        mappedFileMeta.setFileId(fileId.toString())
        mappedFileMeta.setDuration(100L)

        multipartFile.getOriginalFilename() >> originalName
        multipartFile.getContentType() >> contentType
        multipartFile.getSize() >> fileSize

        repository.findById(fileId) >> Optional.of(existingResource)
        storageStrategy.store(multipartFile, fileId, duration) >> updatedResource
        repository.saveAndFlush(_) >> { FileResource fr -> 
            fr.setId(fileId)
            fr.setStatus(FileStatus.NORMAL)
            fr.setStoragePath(storagePath)
            fr.setStorageType(StorageType.LOCAL)
            fr.setHash(fileHash)
            fr.setSize(fileSize)
            fr.setUploadTime(fr.getUploadTime())
            return fr
        }
        mediaFileResourceRepository.findByFileId(fileId) >> null
        mediaFileResourceRepository.save(_) >> mediaResource
        fileMapper.toDTOMedia(_, _) >> fileUploadResponse
        fileMapper.toMetaWithMedia(_, _) >> mappedFileMeta

        when:
        def result = fileStorageService.storeFile(multipartFile, fileId, duration)

        then:
        result == fileUploadResponse
        result.getId() == fileId
        1 * repository.findById(fileId)
        1 * storageStrategy.store(multipartFile, fileId, duration)
        1 * repository.saveAndFlush({ FileResource fr ->
            fr.getId() == fileId &&
            fr.getStatus() == FileStatus.NORMAL &&
            fr.getStoragePath() != null
        })
        1 * mediaFileResourceRepository.save({ MediaFileResource mr ->
            mr.getFile().getId() == fileId &&
            mr.getDuration() == 100.0f
        })
        1 * fileMapper.toDTOMedia(_, _)
    }

    def "storeFile should return simple response when not media file"() {
        given:
        def multipartFile = Mock(MultipartFile)
        def fileId = UUID.randomUUID()
        def duration = null
        def originalName = "test.txt"
        def contentType = "text/plain"
        def fileSize = 1024L
        def fileHash = "abc123"
        def storagePath = "2025/04/25/" + UUID.randomUUID().toString() + ".txt"

        def existingResource = new FileResource()
        existingResource.setId(fileId)
        existingResource.setOriginalName(originalName)
        existingResource.setContentType(contentType)
        existingResource.setSize(fileSize)
        existingResource.setStorageType(StorageType.LOCAL)
        existingResource.setStatus(FileStatus.UPLOADING)
        existingResource.setUploadTime(LocalDateTime.now())
        existingResource.setExtension("txt")

        def updatedResource = new FileResource()
        updatedResource.setId(fileId)
        updatedResource.setOriginalName(originalName)
        updatedResource.setContentType(contentType)
        updatedResource.setSize(fileSize)
        updatedResource.setStorageType(StorageType.LOCAL)
        updatedResource.setStatus(FileStatus.NORMAL)
        updatedResource.setStoragePath(storagePath)
        updatedResource.setHash(fileHash)
        updatedResource.setExtension("txt")

        def fileUploadResponse = new FileUploadResponse()
        fileUploadResponse.setId(fileId)

        multipartFile.getOriginalFilename() >> originalName
        multipartFile.getContentType() >> contentType
        multipartFile.getSize() >> fileSize

        repository.findById(fileId) >> Optional.of(existingResource)
        storageStrategy.store(multipartFile, fileId, duration) >> updatedResource
        repository.saveAndFlush(_) >> { FileResource fr -> 
            fr.setId(fileId)
            fr.setStatus(FileStatus.NORMAL)
            fr.setStoragePath(storagePath)
            fr.setStorageType(StorageType.LOCAL)
            fr.setHash(fileHash)
            fr.setSize(fileSize)
            fr.setUploadTime(fr.getUploadTime())
            return fr
        }
        mediaFileResourceRepository.findByFileId(fileId) >> null
        fileMapper.toDTO(_) >> fileUploadResponse

        when:
        def result = fileStorageService.storeFile(multipartFile, fileId, duration)

        then:
        result == fileUploadResponse
        result.getId() == fileId
        1 * repository.findById(fileId)
        1 * storageStrategy.store(multipartFile, fileId, duration)
        1 * repository.saveAndFlush({ FileResource fr ->
            fr.getId() == fileId &&
            fr.getStatus() == FileStatus.NORMAL &&
            fr.getStoragePath() != null
        })
        0 * mediaFileResourceRepository.save(_)
        1 * fileMapper.toDTO(_)
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
        def e = thrown(FileNotFoundException)
        e.message.contains("File not found")
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
        def e = thrown(FileNotFoundException)
        e.message.contains("文件不存在或不可读")

        cleanup:
        // 确保测试环境干净
        def testFile = fileStorageService.baseDir.resolve("nonexistent.txt")
        if (Files.exists(testFile)) {
            Files.delete(testFile)
        }
    }

    def "mergeChunks should merge all chunks and return response with media info"() {
        given:
        def fileHash = "abc123"
        def originalName = "merged.mp4"
        def fileId = UUID.randomUUID()
        def duration = 150L

        def existingResource = new FileResource()
        existingResource.setId(fileId)
        existingResource.setOriginalName(originalName)
        existingResource.setSize(1024L)
        existingResource.setStorageType(StorageType.LOCAL)
        existingResource.setStatus(FileStatus.UPLOADING)
        existingResource.setUploadTime(LocalDateTime.now())
        existingResource.setExtension("mp4")

        def updatedResource = new FileResource()
        updatedResource.setId(fileId)
        updatedResource.setOriginalName(originalName)
        updatedResource.setSize(2048L)
        updatedResource.setStorageType(StorageType.LOCAL)
        updatedResource.setStatus(FileStatus.NORMAL)
        updatedResource.setStoragePath("2025/04/25/" + UUID.randomUUID().toString() + ".mp4")
        updatedResource.setHash("merged_hash")
        updatedResource.setExtension("mp4")

        def mediaResource = new MediaFileResource()
        mediaResource.setDuration(150.0f)
        mediaResource.setFile(existingResource)

        def fileUploadResponse = new FileUploadResponse()
        fileUploadResponse.setId(fileId)

        and:
        fileStorageService.baseDir = Paths.get(System.getProperty("java.io.tmpdir"))
        fileStorageService.chunkTempDir = Paths.get(System.getProperty("java.io.tmpdir"))

        and:
        def sessionDir = fileStorageService.chunkTempDir.resolve(fileHash)
        Files.createDirectories(sessionDir)
        Files.createFile(sessionDir.resolve("00001.part"))
        Files.createFile(sessionDir.resolve("00002.part"))

        and:
        repository.findById(fileId) >> Optional.of(existingResource)
        storageStrategy.storeMergedFile(fileHash, originalName, fileId, duration, _) >> updatedResource
        repository.save(_) >> { FileResource fr -> 
            fr.setId(fileId)
            fr.setStatus(FileStatus.NORMAL)
            fr.setUploadTime(existingResource.getUploadTime())
            return fr
        }
        mediaFileResourceRepository.findByFileId(fileId) >> null
        mediaFileResourceRepository.save(_) >> mediaResource
        fileMapper.toDTOMedia(_, _) >> fileUploadResponse

        when:
        def result = fileStorageService.mergeChunks(fileHash, originalName, fileId, duration)

        then:
        result == fileUploadResponse
        result.getId() == fileId
        1 * repository.findById(fileId)
        1 * storageStrategy.storeMergedFile(fileHash, originalName, fileId, duration, _)
        1 * repository.save({ FileResource fr ->
            fr.getId() == fileId &&
            fr.getStatus() == FileStatus.NORMAL
        })
        1 * mediaFileResourceRepository.save({ MediaFileResource mr ->
            mr.getFile().getId() == fileId &&
            mr.getDuration() == 150.0f
        })
        1 * fileMapper.toDTOMedia(_, _)

        cleanup:
        Files.deleteIfExists(sessionDir.resolve("00001.part"))
        Files.deleteIfExists(sessionDir.resolve("00002.part"))
        Files.deleteIfExists(sessionDir)
    }

    def "mergeChunks should merge all chunks and return simple response for non-media file"() {
        given:
        def fileHash = "abc123"
        def originalName = "merged.txt"
        def fileId = UUID.randomUUID()
        def duration = null

        def existingResource = new FileResource()
        existingResource.setId(fileId)
        existingResource.setOriginalName(originalName)
        existingResource.setSize(1024L)
        existingResource.setStorageType(StorageType.LOCAL)
        existingResource.setStatus(FileStatus.UPLOADING)
        existingResource.setUploadTime(LocalDateTime.now())
        existingResource.setExtension("txt")

        def updatedResource = new FileResource()
        updatedResource.setId(fileId)
        updatedResource.setOriginalName(originalName)
        updatedResource.setSize(2048L)
        updatedResource.setStorageType(StorageType.LOCAL)
        updatedResource.setStatus(FileStatus.NORMAL)
        updatedResource.setStoragePath("2025/04/25/" + UUID.randomUUID().toString() + ".txt")
        updatedResource.setHash("merged_hash")
        updatedResource.setExtension("txt")

        def fileUploadResponse = new FileUploadResponse()
        fileUploadResponse.setId(fileId)

        and:
        fileStorageService.baseDir = Paths.get(System.getProperty("java.io.tmpdir"))
        fileStorageService.chunkTempDir = Paths.get(System.getProperty("java.io.tmpdir"))

        and:
        def sessionDir = fileStorageService.chunkTempDir.resolve(fileHash)
        Files.createDirectories(sessionDir)
        Files.createFile(sessionDir.resolve("00001.part"))
        Files.createFile(sessionDir.resolve("00002.part"))

        and:
        repository.findById(fileId) >> Optional.of(existingResource)
        storageStrategy.storeMergedFile(fileHash, originalName, fileId, duration, _) >> updatedResource
        repository.save(_) >> { FileResource fr -> 
            fr.setId(fileId)
            fr.setStatus(FileStatus.NORMAL)
            fr.setUploadTime(existingResource.getUploadTime())
            return fr
        }
        mediaFileResourceRepository.findByFileId(fileId) >> null
        fileMapper.toDTO(_) >> fileUploadResponse

        when:
        def result = fileStorageService.mergeChunks(fileHash, originalName, fileId, duration)

        then:
        result == fileUploadResponse
        result.getId() == fileId
        1 * repository.findById(fileId)
        1 * storageStrategy.storeMergedFile(fileHash, originalName, fileId, duration, _)
        1 * repository.save({ FileResource fr ->
            fr.getId() == fileId &&
            fr.getStatus() == FileStatus.NORMAL
        })
        0 * mediaFileResourceRepository.save(_)
        1 * fileMapper.toDTO(_)

        cleanup:
        Files.deleteIfExists(sessionDir.resolve("00001.part"))
        Files.deleteIfExists(sessionDir.resolve("00002.part"))
        Files.deleteIfExists(sessionDir)
    }

    def "getFileMeta should throw FileNotFoundException when file not exists"() {
        given:
        def fileId = UUID.randomUUID()
        repository.findById(fileId) >> Optional.empty()

        when:
        fileStorageService.getFileMeta(fileId)

        then:
        def ex = thrown(FileNotFoundException)
        ex.message.contains("File not found")
        1 * repository.findById(fileId)
        0 * mediaFileResourceRepository.findByFileId(_)
        0 * fileMapper._(*_)
    }

    def "createFilePlaceholder should create file record and return response"() {
        given:
        def request = Mock(UploadFileRequest)
        def fileId = UUID.randomUUID()
        def fileName = "test.txt"
        def fileSize = 1024L
        
        def fileResource = new FileResource()
        fileResource.setId(fileId)
        fileResource.setOriginalName(fileName)
        fileResource.setSize(fileSize)
        fileResource.setExtension("txt")
        fileResource.setContentType("text/plain")
        fileResource.setStatus(FileStatus.UPLOADING)
        
        def fileUploadResponse = new FileUploadResponse()
        fileUploadResponse.setId(fileId)
        
        request.getFileName() >> fileName
        request.getSize() >> fileSize
        repository.save(_) >> fileResource
        fileMapper.toDTO(fileResource) >> fileUploadResponse

        when:
        def result = fileStorageService.createFilePlaceholder(request)

        then:
        result.getId() != null
        result.getFileStatus() == FileStatus.UPLOADING.name()
        1 * repository.save({ FileResource fr ->
            fr.getOriginalName() == fileName &&
            fr.getSize() == fileSize &&
            fr.getStatus() == FileStatus.UPLOADING
        })
        1 * fileMapper.toDTO(_)
    }

    def "loadFileAsBytes should return file content when file exists"() {
        given:
        def fileResource = new FileResource()
        fileResource.setStoragePath("test.txt")
        
        and:
        fileStorageService.baseDir = Paths.get(System.getProperty("java.io.tmpdir"))
        def filePath = fileStorageService.baseDir.resolve("test.txt")
        Files.createFile(filePath)
        Files.write(filePath, "test content".bytes)

        when:
        def result = fileStorageService.loadFileAsBytes(fileResource)

        then:
        result != null
        new String(result) == "test content"

        cleanup:
        Files.deleteIfExists(filePath)
    }

    def "loadFileAsBytes should throw FileNotFoundException when file not exists"() {
        given:
        def fileResource = new FileResource()
        fileResource.setStoragePath("nonexistent.txt")
        
        and:
        fileStorageService.baseDir = Paths.get(System.getProperty("java.io.tmpdir"))

        when:
        fileStorageService.loadFileAsBytes(fileResource)

        then:
        def ex = thrown(FileNotFoundException)
        ex.message.contains("文件不存在或不可读")
    }
}