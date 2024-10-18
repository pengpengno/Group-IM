package com.github.im.common.connect.handler.client;

import com.github.im.common.connect.connection.ConnectionConstants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author pengpeng
 * @description
 * @date 2023/3/8
 */
@Slf4j
@ChannelHandler.Sharable
public class ClientInboundHandler extends ChannelInboundHandlerAdapter {

    private String jwt;

    public ClientInboundHandler(String token){
        jwt = token;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        log.info("channel  register success");
        Channel channel = ctx.channel();
        channel.attr(ConnectionConstants.ROOM_KEY).set("group");
//        channel.attr(ProtocolType.CHANNEL_JWT).set(jwt
//        Account.Authenticate auth = Account.Authenticate.newBuilder()
//                .setJwt(jwt)
//                .build();
//        try {
//            byte[] bytes = auth.toByteArray();
//            int length = bytes.length;
//            ByteBuf buffer = channel.alloc().buffer(8+ length);
////            ByteBuf buf = Unpooled.buffer(8 + length);
//            buffer.writeInt(length);
//            buffer.writeInt(ProtocolType.ProtocolMessageEnum.AUTH_VALUE);
//            buffer.writeBytes(bytes);
//            channel.writeAndFlush(buffer);
////            buffer.release();
//        } catch (Exception e) {
//            log.error("[client] msg encode has error", e);
//        }
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("channel register success");
        Channel channel = ctx.channel();
//        Account.Authenticate auth = Account.Authenticate.newBuilder()
//                .setJwt(jwt)
//                .build();
        try {
//            byte[] bytes = auth.toByteArray();
//            int length = bytes.length;
//            ByteBuf buffer = channel.alloc().buffer(8+ length);
//            ByteBuf buf = Unpooled.buffer(8 + length);
//            buffer.writeInt(length);
//            buffer.writeInt(ProtocolType.ProtocolMessageEnum.AUTH_VALUE);
//            buffer.writeBytes(bytes);
//            channel.writeAndFlush(buffer);
//            buffer.release();
        } catch (Exception e) {
            log.error("[client] msg encode has error", e);
        }
        super.channelActive(ctx);
    }



    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("channel read success   {} ",msg.toString());
        super.channelRead(ctx, msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }
}
