package com.github.im.common.connect.utils;

import cn.hutool.core.exceptions.ExceptionUtil;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ByteBufUtils {



    public  static byte[] readByteBuf2Array (ByteBuf byteBuf){
        try{
            int length = byteBuf.readableBytes();
//            int length = byteBuf.readInt();

//            int type = byteBuf.readInt();

//            ProtocolType.ProtocolMessageEnum messageEnum = ProtocolType.ProtocolMessageEnum.forNumber(type);
//
//            if (messageEnum == null){
//                throw new IllegalAccessException("The send data package is null of type, pls refer to the  ProtocolMessageEnum!");
//            }
            byte[] bytes;

            if (byteBuf.hasArray()) {  //  jvm  heap byteBuf 处理

                bytes = byteBuf.array();

            } else {  //  memory  byteBuf 处理

                bytes = new byte[length];

                byteBuf.getBytes(byteBuf.readerIndex(),bytes);

            }

            return bytes;
        }

        catch (Exception exception){

            log.error("Illegal message {}", ExceptionUtil.stacktraceToString(exception));
//            throw new IllegalAccessException("The send data package is null of type!");
            return null;
        }
    }
}
