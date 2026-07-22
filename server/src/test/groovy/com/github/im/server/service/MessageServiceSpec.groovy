package com.github.im.server.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.im.common.connect.model.proto.Chat
import com.github.im.dto.message.DefaultMessagePayLoad
import com.github.im.dto.message.FileMeta
import com.github.im.dto.message.MessageDTO
import com.github.im.dto.message.MessagePayLoad
import com.github.im.enums.MessageStatus
import com.github.im.enums.MessageType
import com.github.im.server.mapstruct.MessageMapper
import com.github.im.server.model.Conversation
import com.github.im.server.model.Message
import com.github.im.server.model.User
import com.github.im.server.model.enums.FileStatus
import com.github.im.server.repository.GroupMemberRepository
import com.github.im.server.repository.MessageRepository
import com.github.im.server.service.notification.ClientEventPublisher
import jakarta.persistence.EntityManager
import spock.lang.Specification

import java.io.FileNotFoundException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class MessageServiceSpec extends Specification {

    def messageRepository = Mock(MessageRepository)
    def groupMemberRepository = Mock(GroupMemberRepository)
    def messageMapper = Mock(MessageMapper)
    def fileStorageService = Mock(FileStorageService)
    def conversationSequenceService = Mock(ConversationSequenceService)
    def conversationService = Mock(ConversationService)
    def redisMessageRouter = Mock(RedisMessageRouter)
    def objectMapper = Mock(ObjectMapper)
    def clientEventPublisher = Mock(ClientEventPublisher)
    def entityManager = Mock(EntityManager)

    def messageService = new MessageService(
            messageRepository,
            groupMemberRepository,
            messageMapper,
            fileStorageService,
            conversationSequenceService,
            conversationService,
            redisMessageRouter,
            objectMapper,
            clientEventPublisher
    )

    def setup() {
        messageService.@entityManager = entityManager
    }

    def "getMessageById should return converted DTO when message exists"() {
        given:
        def messageId = 1L
        def message = new Message()
        message.setMsgId(messageId)
        message.setType(MessageType.TEXT)
        message.setContent("hello")

        def dto = new MessageDTO<MessagePayLoad>()
        dto.setMsgId(messageId)

        messageRepository.findById(messageId) >> Optional.of(message)
        messageMapper.toDTO(message) >> dto

        when:
        def result = messageService.getMessageById(messageId)

        then:
        result.msgId == messageId
        result.payload instanceof DefaultMessagePayLoad
        result.payload.content == "hello"
    }

    def "getMessageById should throw when message does not exist"() {
        given:
        messageRepository.findById(99L) >> Optional.empty()

        when:
        messageService.getMessageById(99L)

        then:
        thrown(IllegalStateException)
    }

    def "saveMessage should persist mapped status and generated sequence"() {
        given:
        def conversation = new Conversation()
        conversation.setConversationId(10L)
        def user = new User()
        user.setUserId(7L)

        def chatMessage = Chat.ChatMessage.newBuilder()
                .setConversationId(10L)
                .setContent("payload")
                .setClientMsgId("client-1")
                .setType(Chat.MessageType.TEXT)
                .setMessagesStatus(Chat.MessagesStatus.SENDING)
                .setFromUser(com.github.im.common.connect.model.proto.User.UserInfo.newBuilder()
                        .setUserId(7L)
                        .build())
                .setClientTimeStamp(1710000000000L)
                .build()

        entityManager.getReference(Conversation, 10L) >> conversation
        entityManager.getReference(User, 7L) >> user
        conversationSequenceService.nextSequence(10L) >> 88L
        messageRepository.save(_ as Message) >> { Message saved ->
            saved.setMsgId(100L)
            saved
        }

        when:
        def result = messageService.saveMessage(chatMessage)

        then:
        def expectedClientTimestamp = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(1710000000000L),
                ZoneId.systemDefault()
        )
        result.msgId == 100L
        result.clientMsgId == "client-1"
        result.type == MessageType.TEXT
        result.status == MessageStatus.SENT
        result.sequenceId == 88L
        result.clientTimestamp == expectedClientTimestamp
    }

    def "convertMessage should preserve uploading attachment metadata for image messages"() {
        given:
        def fileId = UUID.randomUUID().toString()
        def message = new Message()
        message.setMsgId(2L)
        message.setType(MessageType.IMAGE)
        message.setContent(fileId)

        def dto = new MessageDTO<MessagePayLoad>()
        dto.setMsgId(2L)

        def fileMeta = FileMeta.builder()
                .fileId(fileId)
                .filename("pending.heic")
                .fileSize(2048L)
                .contentType("image/heic")
                .fileStatus(FileStatus.UPLOADING.name())
                .build()

        messageMapper.toDTO(message) >> dto
        fileStorageService.getFileMeta(UUID.fromString(fileId)) >> fileMeta

        when:
        def result = messageService.convertMessage(message)

        then:
        result.payload instanceof FileMeta
        with(result.payload as FileMeta) {
            it.fileId == fileId
            it.filename == "pending.heic"
            it.contentType == "image/heic"
            it.fileStatus == FileStatus.UPLOADING.name()
        }
    }

    def "convertMessage should fallback to raw content when attachment metadata lookup fails"() {
        given:
        def fileId = UUID.randomUUID().toString()
        def message = new Message()
        message.setMsgId(3L)
        message.setType(MessageType.IMAGE)
        message.setContent(fileId)

        def dto = new MessageDTO<MessagePayLoad>()
        dto.setMsgId(3L)

        messageMapper.toDTO(message) >> dto
        fileStorageService.getFileMeta(UUID.fromString(fileId)) >> { throw new FileNotFoundException("missing meta") }

        when:
        def result = messageService.convertMessage(message)

        then:
        result.payload instanceof DefaultMessagePayLoad
        result.payload.content == fileId
    }
}
