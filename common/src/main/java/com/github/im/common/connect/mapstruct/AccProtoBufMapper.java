package com.github.im.common.connect.mapstruct;

import com.github.im.common.connect.model.proto.Chat;
import com.github.im.common.model.account.ChatMsgVo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AccProtoBufMapper {

    AccProtoBufMapper INSTANCE = Mappers.getMapper(AccProtoBufMapper.class);



    default ChatMsgVo tran2ProtoChat(Chat.ChatMessage chatMessage){

        if (chatMessage == null){
            return null;
        }
        ChatMsgVo chatMsgVo = new ChatMsgVo();
        chatMsgVo.setMsgId(chatMessage.getMsgId());
        chatMsgVo.setContent(chatMessage.getContent());
        chatMsgVo.setSessionId(chatMsgVo.getSessionId());
        chatMsgVo.setFromAccount(ProtoBufMapper.INSTANCE.proto2Acc(chatMessage.getFromAccountInfo()));
        chatMsgVo.setMsgSendTime(chatMsgVo.getMsgSendTime());
        chatMsgVo.setContentType(chatMsgVo.getContentType());
        chatMsgVo.setMsgCreateTime(chatMsgVo.getMsgCreateTime());
        return chatMsgVo;
    }


}
