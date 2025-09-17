package com.github.im.server.utils;

import com.github.im.common.connect.model.proto.Chat;
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


    /**
     * 将MessageType枚举类型转换为com.github.im.common.connect.model.proto.Chat.MessageType枚举类型
     *
     * @param messageType 要转换的枚举值，来自某个MessageType枚举
     * @return 对应的com.github.im.common.connect.model.proto.Chat.MessageType枚举值
     */
    public static com.github.im.common.connect.model.proto.Chat.MessageType convertMessageType(MessageType messageType) {
        // 根据输入的MessageType枚举类型，转换并返回对应的com.github.im.common.connect.model.proto.Chat.MessageType枚举类型
        switch (messageType) {
            case TEXT:
                // 转换为文本消息类型
                return com.github.im.common.connect.model.proto.Chat.MessageType.TEXT;
            case FILE:
                // 转换为文件消息类型
                return com.github.im.common.connect.model.proto.Chat.MessageType.FILE;
            case VIDEO:
                // 转换为视频消息类型
                return com.github.im.common.connect.model.proto.Chat.MessageType.VIDEO;
            case IMAGE:
                // 转换为图片消息类型
                return com.github.im.common.connect.model.proto.Chat.MessageType.IMAGE;
            case VOICE:
                // 转换为语音消息类型
                return com.github.im.common.connect.model.proto.Chat.MessageType.VOICE;
            default:
                // 默认转换为文本消息类型
                return com.github.im.common.connect.model.proto.Chat.MessageType.TEXT;
        }
    }

    /**
     * 将枚举类型的MessageStatus转换为ChatMessage的MessagesStatus
     * 此方法用于统一消息状态的转换，将内部枚举类型转换为protobuf定义的枚举类型
     * 这在数据传输和跨系统通信中很有用，确保了数据的一致性和可理解性
     *
     * @param messageStatus 消息状态的枚举类型，表示消息的各种状态（如未发送、已发送等）
     * @return 返回转换后的ChatMessage的MessagesStatus枚举类型
     */
    public static com.github.im.common.connect.model.proto.Chat.MessagesStatus convertMessageStatus(MessageStatus messageStatus) {
        switch (messageStatus) {
//
            case SENT:
                // 当消息状态为已发送时，返回对应的protobuf已发送状态
                return com.github.im.common.connect.model.proto.Chat.MessagesStatus.SENT;

            case SENDING:
                // 当消息状态为发送中时，返回对应的protobuf发送中状态
                return com.github.im.common.connect.model.proto.Chat.MessagesStatus.SENDING;

            case FAILED:
                // 当消息发送失败时，返回对应的protobuf发送失败状态
                return com.github.im.common.connect.model.proto.Chat.MessagesStatus.FAILED;

            case RECEIVED:
                // 当消息已接收时，返回对应的protobuf已接收状态
                return com.github.im.common.connect.model.proto.Chat.MessagesStatus.RECEIVED;

            case READ:
                // 当消息已读时，返回对应的protobuf已读状态
                return com.github.im.common.connect.model.proto.Chat.MessagesStatus.READ;

            case UNREAD:
                // 当消息未读时，返回对应的protobuf未读状态
                return com.github.im.common.connect.model.proto.Chat.MessagesStatus.UNREAD;

            case DELETED:
                // 当消息已删除时，返回对应的protobuf已删除状态
                return com.github.im.common.connect.model.proto.Chat.MessagesStatus.DELETED;
            case REVOKE:
                // 当消息被撤销时，返回对应的protobuf撤销状态
                return com.github.im.common.connect.model.proto.Chat.MessagesStatus.REVOKE;

            default:
                // 默认返回消息未发送状态
                return Chat.MessagesStatus.SENT;
        }
    }

    /**
     * 将protobuf定义的消息类型转换为项目内部使用的消息类型.
     * 此方法确保了系统间消息类型的兼容性与一致性.
     *
     * @param messageType 从protobuf接收到的消息类型.
     * @return 转换后的项目内部消息类型.
     */
    public static MessageType convertMessageType(com.github.im.common.connect.model.proto.Chat.MessageType messageType) {
        // 根据传入的protobuf消息类型，返回对应项目内部消息类型
        switch (messageType) {
            case TEXT:
                return MessageType.TEXT;
            case FILE:
                return MessageType.FILE;
            case VIDEO:
                return MessageType.VIDEO;
            case IMAGE:
                return MessageType.IMAGE;
            case VOICE:
                return MessageType.VOICE;
            // 对于未处理的消息类型，默认为TEXT，确保总是有返回值
            default:
                return MessageType.TEXT;
        }
    }

    /**
     * 将枚举类型的聊天消息状态转换为对应的消息状态
     * 此方法主要用于在不同的消息状态表示方式之间进行转换，确保消息状态在不同的系统或模块之间保持一致
     *
     * @param messageStatus 枚举类型的聊天消息状态，表示消息的各种可能状态
     * @return MessageStatus 根据输入的消息状态返回相应的消息状态枚举值
     */
    public static MessageStatus convertMessageStatus(com.github.im.common.connect.model.proto.Chat.MessagesStatus messageStatus) {
        return switch (messageStatus) {

            case RECEIVED ->
                // 默认返回消息未发送状态
                    MessageStatus.RECEIVED;
            case SENT ->
                // 消息已发送状态
                    MessageStatus.SENT;
            case SENDING ->
                // 默认返回消息发送中状态
                    MessageStatus.SENDING;
            case FAILED ->
                // 默认返回消息发送失败状态
                    MessageStatus.FAILED;
            case DELETED ->
                // 默认返回消息已删除状态
                    MessageStatus.DELETED;
            case REVOKE ->
                // 默认返回消息已撤回状态
                    MessageStatus.REVOKE;
//
            case READ ->
                // 消息已读状态
                    MessageStatus.READ;
            case UNREAD ->
                // 消息未读状态
                    MessageStatus.UNREAD;
            default ->
                // 默认返回消息未发送状态，以确保所有可能的情况都有一个默认处理方式
                    MessageStatus.SENT;
        };
    }
}