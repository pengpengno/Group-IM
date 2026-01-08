package com.github.im.server

import com.github.im.common.connect.model.proto.Chat
import com.github.im.dto.message.*
import com.github.im.enums.MessageStatus
import com.github.im.enums.MessageType
import com.github.im.server.mapstruct.MessageMapper
import com.github.im.server.model.Conversation
import com.github.im.server.model.FileResource
import com.github.im.server.model.Message
import com.github.im.server.model.User
import com.github.im.server.repository.MessageRepository
import com.github.im.server.service.ConversationSequenceService
import com.github.im.server.service.FileStorageService
import com.github.im.server.service.MessageService
import jakarta.persistence.EntityManager
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import spock.lang.Specification

class MessageServiceSpec extends Specification {

    def messageRepository = Mock(MessageRepository)
    def messageMapper = Mock(MessageMapper)
    def fileStorageService = Mock(FileStorageService)
    def conversationSequenceService = Mock(ConversationSequenceService)
    def entityManager = Mock(EntityManager)

    def messageService = new MessageService(
            messageRepository,
            messageMapper,
            fileStorageService,
            conversationSequenceService,
            entityManager
    )

    def "getMessageById should return message DTO when message exists"() {
        given:
        def messageId = 1L
        def message = new Message()
        message.setMessageId(messageId)
        message.setType(MessageType.TEXT)
        message.setContent("Hello World")

        def messageDTO = new MessageDTO<MessagePayLoad>()
        messageDTO.setId(messageId)
        messageDTO.setPayload(new DefaultMessagePayLoad("Hello World"))

        messageRepository.findById(messageId) >> Optional.of(message)
        messageMapper.toDTO(message) >> messageDTO

        when:
        def result = messageService.getMessageById(messageId)

        then:
        result.getId() == messageId
        result.getPayload().getContent() == "Hello World"
    }

    def "getMessageById should throw exception when message not exists"() {
        given:
        def messageId = 1L
        messageRepository.findById(messageId) >> Optional.empty()

        when:
        messageService.getMessageById(messageId)

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "消息不存在"
    }

    def "pullHistoryMessages should return paginated messages"() {
        given:
        def request = new MessagePullRequest()
        request.setConversationId(1L)
        request.setPage(0)
        request.setSize(10)
        request.setSort("createTime")

        def message = new Message()
        message.setMessageId(1L)
        message.setType(MessageType.TEXT)
        message.setContent("Test message")

        def messageDTO = new MessageDTO<MessagePayLoad>()
        messageDTO.setId(1L)
        messageDTO.setPayload(new DefaultMessagePayLoad("Test message"))

        def pageable = PageRequest.of(0, 10, Sort.by("createTime").descending())
        def pageResult = new PageImpl([message], pageable, 1)

        messageRepository.findAll(_, _) >> pageResult
        messageMapper.toDTO(message) >> messageDTO

        when:
        def result = messageService.pullHistoryMessages(request)

        then:
        result.getTotalElements() == 1
        result.getContent().size() == 1
        result.getContent()[0].getId() == 1L
    }

    def "searchMessages should return searched messages"() {
        given:
        def request = new MessageSearchRequest()
        request.setKeyword("test")
        request.setSessionId(1L)

        def message = new Message()
        message.setMessageId(1L)
        message.setType(MessageType.TEXT)
        message.setContent("Test message")

        def messageDTO = new MessageDTO<MessagePayLoad>()
        messageDTO.setId(1L)
        messageDTO.setPayload(new DefaultMessagePayLoad("Test message"))

        def pageable = PageRequest.of(0, 10)
        def pageResult = new PageImpl([message], pageable, 1)

        messageRepository.searchMessages("test", 1L, _) >> pageResult
        messageMapper.toDTO(message) >> messageDTO

        when:
        def result = messageService.searchMessages(request, pageable)

        then:
        result.getTotalElements() == 1
        result.getContent().size() == 1
        result.getContent()[0].getId() == 1L
    }

    def "markAsRead should update message status to READ"() {
        given:
        def messageId = 1L
        def user = new User()
        user.setUserId(1L)

        def message = new Message()
        message.setMessageId(messageId)
        message.setStatus(MessageStatus.SENT)

        messageRepository.findById(messageId) >> Optional.of(message)

        when:
        messageService.markAsRead(messageId, user)

        then:
        1 * messageRepository.save({ Message msg ->
            msg.getStatus() == MessageStatus.READ
        })
    }

    def "saveMessage should save message with proper fields"() {
        given:
        def chatMessage = Chat.ChatMessage.newBuilder()
                .setConversationId(1L)
                .setContent("Hello")
                .setClientMsgId("client123")
                .setType(Chat.MessageType.TEXT)
                .setMessagesStatus(Chat.MessagesStatus.SENDING)
                .build()

        def conversation = new Conversation()
        conversation.setConversationId(1L)

        def user = new User()
        user.setUserId(1L)

        def savedMessage = new Message()
        savedMessage.setMessageId(1L)
        savedMessage.setConversation(conversation)
        savedMessage.setContent("Hello")
        savedMessage.setClientMsgId("client123")
        savedMessage.setType(MessageType.TEXT)
        savedMessage.setStatus(MessageStatus.SENT)
        savedMessage.setSequenceId(100L)

        entityManager.getReference(Conversation, 1L) >> conversation
        entityManager.getReference(User, 1L) >> user
        conversationSequenceService.nextSequence(1L) >> 100L
        messageRepository.save(_) >> savedMessage

        when:
        def result = messageService.saveMessage(chatMessage)

        then:
        result.getMessageId() == 1L
        result.getConversation().getConversationId() == 1L
        result.getContent() == "Hello"
        result.getClientMsgId() == "client123"
        result.getType() == MessageType.TEXT
        result.getStatus() == MessageStatus.SENT
        result.getSequenceId() == 100L
    }

    def "convertMessage should handle TEXT message type"() {
        given:
        def message = new Message()
        message.setMessageId(1L)
        message.setType(MessageType.TEXT)
        message.setContent("Hello World")

        def messageDTO = new MessageDTO<MessagePayLoad>()
        messageDTO.setId(1L)

        messageMapper.toDTO(message) >> messageDTO

        when:
        def result = messageService.convertMessage(message)

        then:
        result.getId() == 1L
        result.getPayload() instanceof DefaultMessagePayLoad
        result.getPayload().getContent() == "Hello World"
    }

    def "convertMessage should handle FILE message type"() {
        given:
        def message = new Message()
        message.setMessageId(1L)
        message.setType(MessageType.FILE)
        message.setContent("file-uuid")

        def messageDTO = new MessageDTO<MessagePayLoad>()
        messageDTO.setId(1L)

        def fileResource = new FileResource()
        fileResource.setOriginalName("test.txt")
        fileResource.setSize(1024L)
        fileResource.setContentType("text/plain")
        fileResource.setHash("abc123")

        messageMapper.toDTO(message) >> messageDTO
        fileStorageService.getFileResourceById("file-uuid") >> fileResource

        when:
        def result = messageService.convertMessage(message)

        then:
        result.getId() == 1L
        result.getPayload() instanceof FileMeta
        result.getPayload().getFilename() == "test.txt"
        result.getPayload().getFileSize() == 1024L
        result.getPayload().getContentType() == "text/plain"
        result.getPayload().getHash() == "abc123"
    }
}