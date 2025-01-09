//package com.github.im.server.handler;
//
//import com.github.im.common.connect.model.proto.BaseMessage;
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.ByteBufAllocator;
//import io.netty.buffer.ByteBufConvertible;
//
//public class ProtobufMessageWrapper implements ByteBufConvertible {
//    private final BaseMessage.BaseMessagePkg message;
//
//    public ProtobufMessageWrapper(BaseMessage.BaseMessagePkg message) {
//        this.message = message;
//    }
//
//
//    @Override
//    public ByteBuf asByteBuf() {
//        byte[] serializedMessage = message.toByteArray();
//        ByteBuf byteBuf = allocator.buffer(serializedMessage.length);
//        byteBuf.writeBytes(serializedMessage);
//        return byteBuf;    }
//}
