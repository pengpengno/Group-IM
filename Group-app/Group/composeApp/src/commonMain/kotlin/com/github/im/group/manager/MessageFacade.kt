package com.github.im.group.manager

import com.github.im.common.connect.model.proto.ChatMessage
import com.github.im.common.connect.model.proto.MessagesStatus
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
import db.FileResource
import db.OfflineMessage
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
        val message = MessageWrapper(
            message = chatMessageBuilder.textMessage(conversationId, content, currentUser.toUserInfo())
        )
        enqueuePreparedMessage(
            currentUser = currentUser,
            messageItem = message,
            toUserId = toUserId,
            pickedFile = null,
            duration = 0L,
            saveToStore = true
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    fun sendFile(conversationId: Long, file: File, duration: Long, currentUser: UserInfo, toUserId: Long?) {
        val localMessage = createLocalPendingFileMessage(
            conversationId = conversationId,
            file = file,
            duration = duration,
            currentUser = currentUser
        )
        store.saveOrUpdate(localMessage)

        scope.launch {
            prepareAndDispatchFileMessage(
                currentUser = currentUser,
                localMessage = localMessage,
                toUserId = toUserId,
                file = file,
                duration = duration
            )
        }
    }

    fun retryMessage(messageItem: MessageItem, currentUser: UserInfo, toUserId: Long?) {
        val retryMessage = (messageItem as? MessageWrapper)?.withStatus(MessageStatus.SENDING)
            ?: MessageWrapper(message = buildResendMessage(messageItem, currentUser))
        store.saveOrUpdate(retryMessage)

        if (!messageItem.type.isFile()) {
            enqueuePreparedMessage(
                currentUser = currentUser,
                messageItem = retryMessage,
                toUserId = toUserId,
                pickedFile = null,
                duration = 0L,
                saveToStore = false
            )
            return
        }

        val localFile = resolveRetryFile(messageItem) ?: run {
            markFailed(messageItem.clientMsgId, messageItem.content)
            return
        }
        val duration = filesRepository.getFileMeta(messageItem.content)?.duration?.toLong() ?: 0L

        scope.launch {
            if (isLocalPendingFileId(messageItem.content)) {
                prepareAndDispatchFileMessage(
                    currentUser = currentUser,
                    localMessage = retryMessage.withContent(messageItem.content),
                    toUserId = toUserId,
                    file = localFile,
                    duration = duration
                )
            } else {
                enqueuePreparedMessage(
                    currentUser = currentUser,
                    messageItem = retryMessage,
                    toUserId = toUserId,
                    pickedFile = localFile,
                    duration = duration,
                    saveToStore = false
                )
            }
        }
    }

    private fun enqueuePreparedMessage(
        currentUser: UserInfo,
        messageItem: MessageWrapper,
        toUserId: Long?,
        pickedFile: File?,
        duration: Long,
        saveToStore: Boolean
    ) {
        if (saveToStore) {
            // 文本消息和“已经拿到 serverFileId 的文件消息”都可以直接进入本地消息流。
            store.saveOrUpdate(messageItem)
        }

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

        scope.launch {
            dispatchSendRequest(currentUser, messageItem, pickedFile, duration)
        }
    }

    private suspend fun prepareAndDispatchFileMessage(
        currentUser: UserInfo,
        localMessage: MessageWrapper,
        toUserId: Long?,
        file: File,
        duration: Long
    ) {
        try {
            val serverFileId = FileApi.createFilePlaceholder(
                UploadFileRequest(
                    size = file.size,
                    fileName = file.name,
                    duration = duration
                )
            ).id

            filesRepository.replaceFileId(
                oldFileId = localMessage.content,
                newFileId = serverFileId
            )

            val preparedMessage = localMessage.withContent(serverFileId)
            store.saveOrUpdate(preparedMessage)

            enqueuePreparedMessage(
                currentUser = currentUser,
                messageItem = preparedMessage,
                toUserId = toUserId,
                pickedFile = file,
                duration = duration,
                saveToStore = false
            )
        } catch (e: Exception) {
            markFailed(localMessage.clientMsgId, localMessage.content)
            Napier.e("prepare file message failed: ${file.name}", e)
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
            if (pickedFile != null && filesRepository.getFile(messageItem.content) == null) {
                filesRepository.addPendingFileRecord(
                    fileId = messageItem.content,
                    fileName = pickedFile.name,
                    duration = duration,
                    filePath = pickedFile.path,
                    contentType = pickedFile.mimeType.orEmpty(),
                    size = pickedFile.size
                )
            }

            // 先发消息体，再单独上传文件；对端据此显示“消息已到，附件仍在准备”的中间态。
            senderSdk.sendMessage(messageItem.message ?: buildResendMessage(messageItem, currentUser))

            if (pickedFile != null) {
                uploadAttachment(messageItem, pickedFile, duration)
            }
        } catch (e: Exception) {
            markFailed(clientMsgId, messageItem.content)
            Napier.e("send message failed: $clientMsgId", e)
        }
    }

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

            store.get(messageItem.clientMsgId)?.let(store::saveOrUpdate)
            clearOfflineIfReady(messageItem.clientMsgId)
        } catch (e: Exception) {
            markFailed(messageItem.clientMsgId, messageItem.content)
            Napier.e("upload attachment failed: ${messageItem.clientMsgId}", e)
        }
    }

    private fun markFailed(clientMsgId: String, fileId: String) {
        offlineMessageRepository.updateOfflineMessageStatus(clientMsgId, MessageStatus.FAILED, incrementRetry = true)
        if (fileId.isNotBlank()) {
            filesRepository.updateFileStatus(fileId, FileStatus.FAILED)
        }
        val msg = store.get(clientMsgId) as? MessageWrapper ?: return
        store.saveOrUpdate(msg.withStatus(MessageStatus.FAILED))
    }

    fun handleAck(clientMsgId: String) {
        val msg = store.get(clientMsgId) as? MessageWrapper ?: return
        store.saveOrUpdate(msg.withStatus(MessageStatus.SENT))
        clearOfflineIfReady(clientMsgId)
    }

    private fun clearOfflineIfReady(clientMsgId: String) {
        val message = store.get(clientMsgId) ?: return
        val delivered = message.status == MessageStatus.SENT ||
            message.status == MessageStatus.READ ||
            message.status == MessageStatus.RECEIVED
        if (!delivered) return

        val uploadCompleted = !message.type.isFile() ||
            filesRepository.getFileMeta(message.content)?.fileStatus == FileStatus.NORMAL
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

    private fun buildResendMessage(item: MessageItem, user: UserInfo) = ChatMessage(
        content = item.content,
        conversationId = item.conversationId,
        fromUser = user.toUserInfo(),
        type = com.github.im.common.connect.model.proto.MessageType.valueOf(item.type.name),
        messagesStatus = MessagesStatus.SENDING,
        clientTimeStamp = Clock.System.now().toEpochMilliseconds(),
        clientMsgId = item.clientMsgId
    )

    private fun buildOfflineRetryMessage(offline: OfflineMessage, user: UserInfo) = MessageWrapper(
        message = ChatMessage(
            content = offline.content,
            conversationId = offline.conversation_id ?: 0L,
            fromUser = user.toUserInfo(),
            type = com.github.im.common.connect.model.proto.MessageType.valueOf(offline.message_type.name),
            messagesStatus = MessagesStatus.SENDING,
            clientTimeStamp = Clock.System.now().toEpochMilliseconds(),
            clientMsgId = offline.client_msg_id
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

    @OptIn(ExperimentalUuidApi::class)
    private fun createLocalPendingFileMessage(
        conversationId: Long,
        file: File,
        duration: Long,
        currentUser: UserInfo
    ): MessageWrapper {
        val clientMsgId = Uuid.random().toString()
        val localFileId = buildLocalPendingFileId(clientMsgId)
        val messageType = FileTypeDetector.getMessageType(
            mimeType = file.mimeType,
            filename = file.name
        )

        filesRepository.addPendingFileRecord(
            fileId = localFileId,
            fileName = file.name,
            duration = duration,
            filePath = file.path,
            contentType = file.mimeType.orEmpty(),
            size = file.size
        )

        return MessageWrapper(
            message = ChatMessage(
                content = localFileId,
                conversationId = conversationId,
                fromUser = currentUser.toUserInfo(),
                type = messageType,
                messagesStatus = MessagesStatus.SENDING,
                clientTimeStamp = Clock.System.now().toEpochMilliseconds(),
                clientMsgId = clientMsgId
            )
        )
    }

    private fun resolveRetryFile(messageItem: MessageItem): File? {
        val fileRecord = filesRepository.getFile(messageItem.content) ?: return null
        return buildFileFromRecord(messageItem.content, fileRecord)
    }

    private fun buildFileFromRecord(fileId: String, fileRecord: FileResource): File {
        return File(
            name = fileRecord.originalName,
            path = fileRecord.storagePath,
            mimeType = fileRecord.contentType.ifBlank { null },
            size = fileRecord.size,
            data = FileData.Path(fileRecord.storagePath.ifBlank { fileId })
        )
    }

    private fun buildLocalPendingFileId(clientMsgId: String): String = "$LOCAL_FILE_PREFIX$clientMsgId"

    private fun isLocalPendingFileId(fileId: String): Boolean = fileId.startsWith(LOCAL_FILE_PREFIX)

    companion object {
        private const val LOCAL_FILE_PREFIX = "local-file:"
    }
}
