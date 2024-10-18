package com.github.im.server.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class RtspRouteHandler extends ChannelInboundHandlerAdapter {



    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("channel read success   {} ",msg.toString());

//        1. find out the meta room key via ctx ??
//        2. check the meta room ket is existed？
//        3. route the data to for specified which is in the same room
//        call the super for next handler read data is necessart ?
//        i mean  that is there some encoder or decoder for rtsp data like RtspEncode?
        super.channelRead(ctx, msg);
    }



}
