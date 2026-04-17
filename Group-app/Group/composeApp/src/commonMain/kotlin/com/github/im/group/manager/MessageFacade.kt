package com.github.im.group.manager

import com.github.im.group.api.FileApi
import com.github.im.group.api.UploadFileRequest
import com.github.im.group.db.entities.FileStatus
import com.github.im.group.db.entities.MessageStatus
import com.github.im.group.model.MessageItem
import com.github.im.group.model.MessageWrapper
import com.github.im.group.model.UserInfo
import com.github.im.group.model.toUserInfo
import com.github.im.group.repository.FilesRepository
import com.github.im.group.repository.OfflineMessageRepository
import com.github.im.group.sdk.ChatMessageBuilder
import com.github.im.group.sdk.File
import com.github.im.group.sdk.FileData
import com.github.im.group.sdk.FilePicker
import com.github.im.group.sdk.SenderSdk
import db.OfflineMessage
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * V8 Architecture: MessageFacade
 * 统一所有消息的发送、重试逻辑、离线消息流、文件上传流。切断与 ViewModel 的耦合。
 */
class MessageFacade(
    private val store: MessageStore,
    private val offlineMessageRepository: OfflineMessageRepository,
    private val filesRepository: FilesRepository,
    private val filePicker: FilePicker,
    private val fileUploadService: FileUploadService,
    private val senderSdk: SenderSdk,
    private val chatMessageBuilder: ChatMessageBuilder
) {

    private val scope = CoroutineScope(Dispatchers.Default)

    fun sendText(conversationId: Long, content: String, currentUser: UserInfo, toUserId: Long?) {
        // Builder 现在是纯同步调用，userInfo 由 Facade 传入
        val message = MessageWrapper(
            message = chatMessageBuilder.textMessage(conversationId, content, currentUser.toUserInfo())
        )
        send(currentUser, message, toUserId, null, 0L)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun sendFile(conversationId: Long, file: File, duration: Long, currentUser: UserInfo, toUserId: Long?) {
        // 文件消息的 proto 构建职责归属 Facade：
        // - FileTypeDetector 推断消息类型
        // - LOCAL_ 占位符由 Facade 生成，在 dispatchSendRequest 阶段换取真实 serverFileId
        val localFileId = "LOCAL_${Uuid.random()}"
        val messageType = FileTypeDetector.getMessageType(filename = file.name)
        val message = MessageWrapper(
            message = chatMessageBuilder.buildMessage(conversationId, localFileId, messageType, currentUser.toUserInfo())
        )
        send(currentUser, message, toUserId, file, duration)
    }

    private fun send(
        currentUser: UserInfo,
        messageItem: MessageWrapper,
        toUserId: Long?,
        pickedFile: File?,
        duration: Long
    ) {
        // 1. Initial Store (Save pending states to prevent losing history)
        store.saveOrUpdate(messageItem)

        // 2. Persist to Offline Queue (Crash recovery)
        offlineMessageRepository.saveOfflineMessage(
            clientMsgId = messageItem.clientMsgId,
            conversationId = messageItem.conversationId,
            fromUserId = currentUser.userId,
            toUserId = toUserId,
            content = messageItem.content,
            messageType = messageItem.type,
            filePath = pickedFile?.path,
            fileSize = pickedFile?.size,
            fileDuration = duration.toInt()
        )

        // 3. Trigger Async Dispatch
        scope.launch {
            dispatchSendRequest(currentUser, messageItem, pickedFile, duration)
        }
    }

    private suspend fun dispatchSendRequest(
        currentUser: UserInfo,
        messageItem: MessageWrapper,
        pickedFile: File?,
        duration: Long
    ) {
        val clientMsgId = messageItem.clientMsgId
        offlineMessageRepository.updateOfflineMessageStatus(clientMsgId, MessageStatus.SENDING)

        try {
            // Resolve server fileId if using a LOCAL_ placeholder (offline-safe pattern)
            val resolvedMessage: MessageWrapper = if (pickedFile != null && messageItem.content.startsWith("LOCAL_")) {
                val uploadRequest = UploadFileRequest(
                    size = pickedFile.size,
                    fileName = pickedFile.name,
                    duration = duration
                )
                val serverFileId = FileApi.createFilePlaceholder(uploadRequest).id
                filesRepository.addPendingFileRecord(
                    fileId = serverFileId,
                    fileName = pickedFile.name,
                    duration = duration,
                    filePath = pickedFile.path
                )
                // Re-build the proto message with the real server fileId
                val updatedProto = messageItem.message?.copy(content = serverFileId)
                MessageWrapper(message = updatedProto).also { store.saveOrUpdate(it) }
            } else {
                if (pickedFile != null && filesRepository.getFile(messageItem.content) == null) {
                    filesRepository.addPendingFileRecord(
                        fileId = messageItem.content,
                        fileName = pickedFile.name,
                        duration = duration,
                        filePath = pickedFile.path
                    )
                }
                messageItem
            }

            senderSdk.sendMessage(resolvedMessage.message ?: buildResendMessage(resolvedMessage, currentUser))

            if (pickedFile != null) {
                uploadAttachment(resolvedMessage, pickedFile, duration)
            }
        } catch (e: Exception) {
            markFailed(clientMsgId, messageItem.content)
            Napier.e("send message failed: $clientMsgId", e)
        }
    }

    /**
     * 上传附件
     */
    private suspend fun uploadAttachment(messageItem: MessageWrapper, file: File, duration: Long) {
        try {
            val data = filePicker.readFileBytes(file)
            val response = fileUploadService.uploadFileData(
                serverFileId = messageItem.content,
                data = data,
                fileName = file.name,
                duration = duration
            )
            response.fileMeta?.let { filesRepository.addOrUpdateFile(it) }
            
            store.get(messageItem.clientMsgId)?.let { currentMsg ->
                store.saveOrUpdate(currentMsg) // Auto-emits via StateFlow
            }
            clearOfflineIfReady(messageItem.clientMsgId)
        } catch (e: Exception) {
            markFailed(messageItem.clientMsgId, messageItem.content)
            Napier.e("upload attachment failed: ${messageItem.clientMsgId}", e)
        }
    }

    private fun markFailed(clientMsgId: String, fileId: String) {
        offlineMessageRepository.updateOfflineMessageStatus(clientMsgId, MessageStatus.FAILED, incrementRetry = true)
        if (fileId.isNotBlank()) filesRepository.updateFileStatus(fileId, FileStatus.FAILED)
        val msg = store.get(clientMsgId) as? MessageWrapper
        if (msg != null) {
            val failedMsg = msg.withStatus(MessageStatus.FAILED)
            store.saveOrUpdate(failedMsg)
        }
    }

    fun handleAck(clientMsgId: String) {
        val msg = store.get(clientMsgId) as? MessageWrapper ?: return
        val sentMsg = msg.withStatus(MessageStatus.SENT)
        store.saveOrUpdate(sentMsg)
        clearOfflineIfReady(clientMsgId)
    }

    private fun clearOfflineIfReady(clientMsgId: String) {
        val message = store.get(clientMsgId) ?: return
        val delivered = message.status == MessageStatus.SENT || message.status == MessageStatus.READ || message.status == MessageStatus.RECEIVED
        if (!delivered) return

        val uploadCompleted = !message.type.isFile() || filesRepository.getFileMeta(message.content)?.fileStatus == FileStatus.NORMAL
        if (uploadCompleted) {
            offlineMessageRepository.deleteOfflineMessage(clientMsgId)
        } else {
            offlineMessageRepository.updateOfflineMessageStatus(clientMsgId, MessageStatus.SENT)
        }
    }

    fun startSync(currentUser: UserInfo) {
        scope.launch {
            val pendings = offlineMessageRepository.getPendingOfflineMessages()
            pendings.forEach { offline ->
                val retryCount = offline.retry_count ?: 0L
                val maxRetryCount = offline.max_retry_count ?: 3L
                if (retryCount >= maxRetryCount) {
                    offlineMessageRepository.updateOfflineMessageStatus(offline.client_msg_id, MessageStatus.FAILED)
                    return@forEach
                }

                val retryMsg = buildOfflineRetryMessage(offline, currentUser)
                val retryFile = if (offline.message_type.isFile()) buildOfflineRetryFile(offline) else null
                
                if (store.get(offline.client_msg_id) == null) {
                    store.saveOrUpdate(retryMsg)
                }

                dispatchSendRequest(currentUser, retryMsg, retryFile, offline.file_duration ?: 0L)
            }
        }
    }

    private fun buildResendMessage(item: MessageItem, user: UserInfo) = com.github.im.common.connect.model.proto.ChatMessage(
        content = item.content, conversationId = item.conversationId, fromUser = user.toUserInfo(),
        type = com.github.im.common.connect.model.proto.MessageType.valueOf(item.type.name),
        messagesStatus = com.github.im.common.connect.model.proto.MessagesStatus.SENDING,
        clientTimeStamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(), clientMsgId = item.clientMsgId
    )

    private fun buildOfflineRetryMessage(offline: OfflineMessage, user: UserInfo) = MessageWrapper(
        message = com.github.im.common.connect.model.proto.ChatMessage(
            content = offline.content, conversationId = offline.conversation_id ?: 0L, fromUser = user.toUserInfo(),
            type = com.github.im.common.connect.model.proto.MessageType.valueOf(offline.message_type.name),
            messagesStatus = com.github.im.common.connect.model.proto.MessagesStatus.SENDING,
            clientTimeStamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(), clientMsgId = offline.client_msg_id
        )
    )

    private fun buildOfflineRetryFile(offline: OfflineMessage): File? {
        val path = offline.file_path ?: return null
        return File(
            name = path.substringAfterLast('/').substringAfterLast('\\').ifBlank { offline.content }, 
            path = path, 
            mimeType = null, 
            size = offline.file_size ?: 0L, 
            data = FileData.Path(path)
        )
    }
}
