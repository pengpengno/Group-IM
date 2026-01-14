package com.github.im.server.ai;

import com.github.im.dto.message.MessageDTO;

/**
 * AI模块的消息包装类，基于系统现有的MessageDTO
 */
public class Message {
    private MessageDTO<?> messageDTO;
    
    public Message(MessageDTO<?> messageDTO) {
        this.messageDTO = messageDTO;
    }
    
    // 代理方法，将调用转发到底层的MessageDTO
    public String getContent() {
        return messageDTO.getContent();
    }
    
    public Long getSenderId() {
        return messageDTO.getFromAccountId();
    }
    
    public Long getConversationId() {
        return messageDTO.getConversationId();
    }
    
    public String getMessageId() {
        return messageDTO.getClientMsgId();
    }
    
    public MessageType getType() {
        return convertToMessageType(messageDTO.getType());
    }
    
    private MessageType convertToMessageType(com.github.im.enums.MessageType originalType) {
        // 转换原始的消息类型到AI模块的消息类型
        switch (originalType) {
            case TEXT:
                return MessageType.TEXT;
            case IMAGE:
                return MessageType.IMAGE;
            case VOICE:
                return MessageType.VOICE;
            case VIDEO:
                return MessageType.VIDEO;
            case FILE:
                return MessageType.FILE;
            default:
                return MessageType.TEXT; // 默认为文本类型
        }
    }
    
    // Getter和Setter
    public MessageDTO<?> getMessageDTO() {
        return messageDTO;
    }
    
    public void setMessageDTO(MessageDTO<?> messageDTO) {
        this.messageDTO = messageDTO;
    }
}