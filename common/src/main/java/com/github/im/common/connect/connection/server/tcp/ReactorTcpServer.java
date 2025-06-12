package com.github.im.common.connect.connection.server.tcp;

import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.common.connect.spi.ReactiveHandlerSPI;
import com.github.im.common.connect.connection.server.ReactiveServer;
import com.github.im.common.connect.connection.ConnectionConstants;
import com.github.im.common.util.RtspServer;
import com.google.inject.Singleton;
import io.netty.channel.ChannelOption;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.codec.rtsp.RtspEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * 响应式 tcp 链接
 * @author pengpeng
 * @description
 * @date 2023/3/3
 */
@Slf4j
@Singleton
public class ReactorTcpServer implements ReactiveServer {

    private final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private InetSocketAddress address;
    private enum SingleInstance{
        INSTANCE;
        private final ReactiveServer instance;
        SingleInstance(){
            instance = new ReactorTcpServer();
        }
        private ReactiveServer getInstance(){
            return instance;
        }
    }
    public static ReactiveServer getInstance(){
        return SingleInstance.INSTANCE.getInstance();
    }

    private TcpServer server;

    private DisposableServer disposableServer;

    private final ProtobufVarint32FrameDecoder protobufVarint32FrameDecoder = new ProtobufVarint32FrameDecoder();
    private final ProtobufDecoder  protobufDecoder = new ProtobufDecoder(BaseMessage.BaseMessagePkg.getDefaultInstance());
    private final ProtobufVarint32LengthFieldPrepender  protobufVarint32LengthFieldPrepender = new ProtobufVarint32LengthFieldPrepender();
    private final ProtobufEncoder  protobufEncoder = new ProtobufEncoder();

    private ReactorTcpServer(){
    }

    @Override
    public void stop() {

        allChannels.disconnect();
        allChannels.close();
        if(disposableServer != null ){
            disposableServer.disposeNow();
        }
    }


    public ReactiveServer init(InetSocketAddress address){
        this.address = address;

        server = TcpServer
                .create()
                .wiretap("tcp-server", LogLevel.INFO)
                .port(address.getPort())
//                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .doOnConnection(connection -> {
                    allChannels.add(connection.channel()); // 将连接添加到管理组
//                    connection.channel().attr(ConnectionConstants.ROOM_KEY).set("group");
//                    connection
////                            .addHandlerFirst(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
//                            .addHandlerLast(new ProtobufVarint32FrameDecoder())
//                            .addHandlerLast(protobufDecoder)
//                            .addHandlerLast(protobufVarint32LengthFieldPrepender)
//                            .addHandlerLast(protobufEncoder)
//                            ;
                })
                .doOnChannelInit((observer, channel, remoteAddress) ->   channel.pipeline()
                        .addFirst(new LoggingHandler("reactor.netty")))

                //  注入 执行的handler
                .handle(ReactiveHandlerSPI.wiredSpiHandler().handler())

                .doOnUnbound(bound -> log.warn(" do on unbound!"))
        ;

        log.info("config netty  on port {}",address.getPort());

        return this;
    }


    public ReactiveServer start(){
        if(address == null || address.isUnresolved()){
            log.error("start server error ");
        }

        log.info("start netty server on port {}",address.getPort());
        disposableServer = server.bindNow();
        disposableServer.onDispose().block();
        return this;
    }

    @Override
    public boolean isRunning() {
        if(disposableServer == null){
            return false;
        }
        return disposableServer.isDisposed();
    }
}
