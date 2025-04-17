package com.github.im.server.utils;

import com.github.im.enums.MessageStatus;
import com.github.im.enums.MessageType;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/4/17
 */
public class EnumsTransUtil {


    public static com.github.im.common.connect.model.proto.Chat.MessageType convertMessageType(MessageType messageType) {
        switch (messageType) {
            case TEXT:
                return com.github.im.common.connect.model.proto.Chat.MessageType.TEXT;
            case FILE:
                return com.github.im.common.connect.model.proto.Chat.MessageType.FILE;
            case STREAM:
                return com.github.im.common.connect.model.proto.Chat.MessageType.STREAM;
            case VIDEO:
                return com.github.im.common.connect.model.proto.Chat.MessageType.VIDEO;
            case MARKDOWN:
                return com.github.im.common.connect.model.proto.Chat.MessageType.MARKDOWN;
            default:
                return com.github.im.common.connect.model.proto.Chat.MessageType.TEXT;
        }
    }

    public static com.github.im.common.connect.model.proto.Chat.ChatMessage.MessagesStatus convertMessageStatus(MessageStatus messageStatus) {
        switch (messageStatus) {
            case UNSENT:
                return com.github.im.common.connect.model.proto.Chat.ChatMessage.MessagesStatus.UNSENT;
            case SENT:
                return com.github.im.common.connect.model.proto.Chat.ChatMessage.MessagesStatus.SENT;
            case SENTFAIL:
                return com.github.im.common.connect.model.proto.Chat.ChatMessage.MessagesStatus.SENTFAIL;
            case HISTORY:
                return com.github.im.common.connect.model.proto.Chat.ChatMessage.MessagesStatus.HISTORY;
            case READ:
                return com.github.im.common.connect.model.proto.Chat.ChatMessage.MessagesStatus.READ;
            case UNREAD:
                return com.github.im.common.connect.model.proto.Chat.ChatMessage.MessagesStatus.UNREAD;
            case OFFLINE:
                return com.github.im.common.connect.model.proto.Chat.ChatMessage.MessagesStatus.OFFLINE;
                default:
                    return com.github.im.common.connect.model.proto.Chat.ChatMessage.MessagesStatus.UNSENT;
        }
    }

    public static MessageType convertMessageType(com.github.im.common.connect.model.proto.Chat.MessageType messageType) {
        switch (messageType) {
            case TEXT:
                return MessageType.TEXT;
            case FILE:
                return MessageType.FILE;
            case STREAM:
                return MessageType.STREAM;
            case VIDEO:
                return MessageType.VIDEO;
            case MARKDOWN:
                return MessageType.MARKDOWN;
           default:
               return MessageType.TEXT;
        }
    }

    public static MessageStatus convertMessageStatus(com.github.im.common.connect.model.proto.Chat.ChatMessage.MessagesStatus messageStatus) {
        return switch (messageStatus) {
            case UNSENT -> MessageStatus.UNSENT;
            case SENT -> MessageStatus.SENT;
            case SENTFAIL -> MessageStatus.SENTFAIL;
            case HISTORY -> MessageStatus.HISTORY;
            case READ -> MessageStatus.READ;
            case UNREAD -> MessageStatus.UNREAD;
            case OFFLINE -> MessageStatus.OFFLINE;
            default -> MessageStatus.UNSENT;
        };
    }
}