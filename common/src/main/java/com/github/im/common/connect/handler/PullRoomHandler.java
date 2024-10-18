package com.github.im.common.connect.handler;

import cn.hutool.core.util.RandomUtil;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

@ChannelHandler.Sharable
public class PullRoomHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;

            if (request.uri().equals("/room")){
                String content = RandomUtil.randomString(8); // Your response content
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                        ctx.alloc().buffer().writeBytes(content.getBytes(CharsetUtil.UTF_8)));
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                ChannelFuture f = ctx.writeAndFlush(response);
                f.addListener(ChannelFutureListener.CLOSE);
            }

        }
        super.channelRead(ctx, msg);
    }
}
