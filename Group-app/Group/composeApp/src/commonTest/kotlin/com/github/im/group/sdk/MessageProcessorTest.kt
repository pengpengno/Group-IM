package com.github.im.group.sdk

import com.github.im.group.api.FileMeta
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.proto.MessageType
import com.github.im.group.model.proto.MessagesStatus
import com.github.im.group.repository.ChatMessageRepository
import com.github.im.group.repository.FilesRepository
import com.github.im.group.repository.UserRepository
import com.github.im.group.model.UserInfo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MockUserRepository : UserRepository(null) {
    override suspend fun withLoggedInUser(block: suspend (UserInfo) -> String): String {
        val userInfo = UserInfo(
            userId = "test_user",
            username = "test_user",
            accountInfo = com.github.im.group.model.proto.AccountInfo(
                account = "test_account",
                username = "test_user",
                userId = "test_user_id"
            )
        )
        return block(userInfo)
    }
}

class MockFilesRepository : FilesRepository(null) {
    override fun addFile(metaFileMeta: FileMeta) {
        // Mock implementation
    }

    override fun addFiles(fileMetas: List<FileMeta>) {
        // Mock implementation
    }

    // Implement other required methods with minimal implementation
    override fun getFile(fileId: String) = null
    override fun updateStoragePath(fileId: String, storagePath: String) {}
    override fun updateLastAccessTime(fileId: String) {}
    override fun updateFileStatus(fileId: String, status: com.github.im.group.db.entities.FileStatus) {}
    override fun updateMediaResourceInfo(fileId: String, thumbnail: String?, duration: Long?) {}
    override fun getExpiredFiles(thresholdTime: kotlinx.datetime.LocalDateTime) = emptyList()
    override fun isFileStoredLocally(fileId: String) = false
    override fun getFileMeta(fileId: String): FileMeta? = null
}

class MockChatMessageRepository : ChatMessageRepository(null, null) {
    private val messages = mutableListOf<MessageWrapper>()
    
    override fun insertOrUpdateMessage(messageItem: com.github.im.group.model.MessageItem) {
        // Mock implementation
    }
    
    override fun getMessageByClientMsgId(clientMsgId: String): com.github.im.group.model.MessageItem? {
        return messages.find { it.clientMsgId == clientMsgId }
    }
    
    // Implement other required methods with minimal implementation
}

class MockFilePicker : FilePicker {
    override suspend fun pickImage() = emptyList<File>()
    override suspend fun pickVideo() = emptyList<File>()
    override suspend fun pickFile() = emptyList<File>()
    override suspend fun takePhoto() = null
    override suspend fun readFileBytes(file: String): ByteArray = null
}

class MessageProcessorTest {
    
    @Test
    fun testTextMessageProcessor() = runTest {
        val userRepository = MockUserRepository()
        val processor = TextMessageProcessor(userRepository)
        
        val result = processor.processMessage(1L, "Hello, World!", MessageType.TEXT, 0)
        
        assertEquals("Hello, World!", result.content)
        assertEquals(MessageType.TEXT, result.type)
        assertEquals(MessagesStatus.SENDING, result.status)
    }
    
    @Test
    fun testFileTypeDetector() {
        // Test image detection
        assertTrue(FileTypeDetector.isImageFile("image/jpeg", "test.jpg"))
        assertTrue(FileTypeDetector.isImageFile(null, "test.png"))
        assertTrue(FileTypeDetector.isImageFile(null, "test.PNG")) // Test case insensitivity
        
        // Test audio detection
        assertTrue(FileTypeDetector.isAudioFile("audio/mpeg", "test.mp3"))
        assertTrue(FileTypeDetector.isAudioFile(null, "test.wav"))
        
        // Test video detection
        assertTrue(FileTypeDetector.isVideoFile("video/mp4", "test.mp4"))
        assertTrue(FileTypeDetector.isVideoFile(null, "test.avi"))
        
        // Test message type detection
        assertEquals(MessageType.IMAGE, FileTypeDetector.getMessageType("image/jpeg", "test.jpg"))
        assertEquals(MessageType.VOICE, FileTypeDetector.getMessageType("audio/mpeg", "test.mp3"))
        assertEquals(MessageType.VIDEO, FileTypeDetector.getMessageType("video/mp4", "test.mp4"))
        assertEquals(MessageType.FILE, FileTypeDetector.getMessageType("application/pdf", "test.pdf"))
    }
    
    @Test
    fun testMessageProcessorFactory() {
        val userRepository = MockUserRepository()
        val filePicker = MockFilePicker()
        val filesRepository = MockFilesRepository()
        val chatMessageRepository = MockChatMessageRepository()
        
        val factory = MessageProcessorFactory(
            userRepository,
            filePicker,
            filesRepository,
            chatMessageRepository
        )
        
        val textProcessor = factory.createProcessor(MessageType.TEXT)
        assertTrue(textProcessor is TextMessageProcessor)
        
        val fileProcessor = factory.createProcessor(MessageType.FILE)
        assertTrue(fileProcessor is FileMessageProcessor)
        
        val voiceProcessor = factory.createProcessor(MessageType.VOICE)
        assertTrue(voiceProcessor is FileMessageProcessor)
    }
}