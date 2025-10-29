package com.github.im.common.connect.handler;

import com.github.im.common.connect.model.proto.BaseMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import static io.netty.handler.timeout.IdleStateEvent.*;

/**
 * 心跳处理 Handler
 */
@Slf4j
public  class HeartbeatHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent event) {

                IdleState state = event.state();
            switch (state){
                case READER_IDLE:
                    // 如果读取超时，则发送心跳请求
                    BaseMessage.BaseMessagePkg heartbeat = BaseMessage.BaseMessagePkg.newBuilder()
                            .setHeartbeat(BaseMessage.Heartbeat.newBuilder().setPing(true).build())
                            .build();
                    ctx.writeAndFlush(heartbeat);
                    log.debug("Sent HEARTBEAT to client");
                    break;
                case WRITER_IDLE:
                    log.debug("Received WRITER_IDLE_STATE_EVENT");
                    break;
                case ALL_IDLE:
                    log.debug("Received ALL_IDLE_STATE_EVENT");
                    break;
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception in HeartbeatHandler", cause);
        ctx.close();
    }
}