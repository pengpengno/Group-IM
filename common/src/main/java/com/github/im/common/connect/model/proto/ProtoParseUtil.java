package com.github.im.common.connect.model.proto;

import com.github.im.common.connect.enums.ProtocolMessageMapEnum;
import com.google.protobuf.Message;
import io.netty.buffer.ByteBuf;

/**
 * @author pengpeng
 * @description
 * @date 2023/3/13
 */
public class ProtoParseUtil {


    /***
     * 将 message 写入 ByteBuf
     * @param message
     * @param buffer
     * @return
     */
    public static ByteBuf parseMessage2ByteBuf(Message message,ByteBuf buffer){
        if (null != message){
            ProtocolMessageMapEnum mapEnum = ProtocolMessageMapEnum.getByClass(message.getClass());
            int type = mapEnum.getTypeEnum().getNumber();
            byte[] bytes = message.toByteArray();
            int length = bytes.length;
            if (null != buffer){
                buffer.writeInt(length);
                buffer.writeInt(type);
                buffer.writeBytes(bytes);
            }
        }
        return buffer;

    }
}
