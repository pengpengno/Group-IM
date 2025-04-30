package com.github.im.group.gui.controller.desktop;

import com.github.im.common.connect.model.proto.Chat;
import com.github.im.dto.session.MessageDTO;
import com.github.im.dto.session.MessagePayLoad;
import com.github.im.enums.MessageType;
import com.github.im.group.gui.util.EnumsTransUtil;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * Description: 消息的帮助类 ，用来再客户端解决不同 message 数据结构间的抽象处理
 * <p>
 * </p>
 * 其中得两个参数 message 和 messageDTO 只能 其中一个为非空，另一个为空
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/4/30
 */
@Getter
public class MessageWrapper implements AbstractMessageHelper{
    private Chat.ChatMessage message;
    private MessageDTO<MessagePayLoad> messageDTO;

    private MessageWrapper(){

    }

    public MessageWrapper(@NotNull Chat.ChatMessage message) {
        if (message == null){
            throw new IllegalArgumentException("message is null");
        }
        this.message = message;
        messageDTO = null;
    }

    public MessageWrapper(@NotNull MessageDTO<MessagePayLoad> messageDTO) {
        if (messageDTO == null){
            throw new IllegalArgumentException("messageDTO is null");
        }
        this.messageDTO = messageDTO;
        message = null ;
    }

    public String getSenderAccount(){
        if (message != null){
            if (message.hasFromAccountInfo()){
                return message.getFromAccountInfo().getAccount();
            }
        }
        if (messageDTO != null){
            return messageDTO.getFromAccount().getUsername();
        }
        throw new IllegalArgumentException("message is null");
    }



    public long getMessageId(){
        if (message != null){
            return message.getMsgId();
        }
        if (messageDTO != null){
            return messageDTO.getMsgId();
        }
        return 0;
    }


    public  long getConversationId() {
        if (message != null){
            return message.getConversationId();
        }
        if (messageDTO != null){
            return messageDTO.getConversationId();
        }
        return 0;
    }

    public MessageType getMessageType() {
        if (message != null){
            return EnumsTransUtil.convertMessageType(message.getType());
        }
        if (messageDTO != null){
            return messageDTO.getType();
        }
        return MessageType.TEXT;
    }



    public long getFromAccountId() {
        if (message != null){
            if (message.hasFromAccountInfo()){
                return message.getFromAccountInfo().getUserId();
            }
        }
        if (messageDTO != null){
            return messageDTO.getFromAccountId();
        }
        return 0;
    }
    public String getContent() {

        if (message != null){
            return message.getContent();
        }

        if (messageDTO != null){
            return messageDTO.getContent();
        }
        return "";
    }


}