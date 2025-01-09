package com.github.im.common.connect.connection.client.tcp;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.github.im.common.connect.connection.client.ClientLifeStyle;
import com.github.im.common.connect.connection.client.ReactiveClientAction;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.common.connect.spi.ReactiveHandlerSPI;
import com.google.protobuf.Message;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.Connection;
import reactor.netty.NettyOutbound;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * reactor 实现客户端
 * @author pengpeng
 * @date 2023/1/8
 */
@Slf4j
public class ReactorTcpClient implements ClientLifeStyle, ReactiveClientAction {

    private Connection connection = null;

    private Disposable disposable = null;

    private InetSocketAddress address;

    private TcpClient client;


    private final ProtobufDecoder  protobufDecoder = new ProtobufDecoder(BaseMessage.BaseMessagePkg.getDefaultInstance());
    private final ProtobufVarint32LengthFieldPrepender  protobufVarint32LengthFieldPrepender = new ProtobufVarint32LengthFieldPrepender();
    private final ProtobufEncoder  protobufEncoder = new ProtobufEncoder();

    @Override
    public ClientLifeStyle config(InetSocketAddress address) {
        log.debug("connect config  {}" ,address);
        this.address = address;
        client = TcpClient
                    .create()
                    .wiretap("tcp-client", LogLevel.INFO)
                    .host(this.address.getHostString())
                    .port(this.address.getPort())
                    .doOnChannelInit((connectionObserver, channel, remoteAddress) -> {
                        log.debug("init channel pipeline ");
//                        ChannelPipeline pipeline = channel.pipeline();
//                        pipeline.addLast(new ProtobufVarint32FrameDecoder())
//                                .addLast(protobufDecoder)
//                                .addLast(protobufVarint32LengthFieldPrepender)
//                                .addLast(protobufEncoder)
//                                ;

                    })
                    .doOnConnected(connection-> {
                        // 建立完 channel 的handler
                        connection
                                .addHandlerLast(new ProtobufVarint32FrameDecoder())
                                .addHandlerLast(protobufDecoder)
                                .addHandlerLast(protobufVarint32LengthFieldPrepender)
                                .addHandlerLast(protobufEncoder)
                        ;

                    })
                    .handle(ReactiveHandlerSPI.wiredSpiHandler().handler())
//                    .handle(((nettyInbound, nettyOutbound) -> {
//                        log.debug("connect server {}  port {} success",address.getHostString(),address.getPort());
//                        Flux<BaseMessage.BaseMessagePkg> subscribe = nettyInbound.receiveObject()
//                                .cast(BaseMessage.BaseMessagePkg.class)
//                                .handle((obj,sink)->{
//                                    log.debug("receive message {}",obj);
//                                })
////                                .doOnError(ex->log.error("receive message error , stack is \n {}", ExceptionUtil.stacktraceToString(ex)))
//                                ;
////                        subscribe.doOnError();
//                        return nettyOutbound.sendObject(Flux.concat(subscribe)).then()
//                                .doOnError(ex->log.error("out bound send essage error , stack is \n {}", ExceptionUtil.stacktraceToString(ex)))
//
//                                ;
//                    }))

        ;
        return this;
    }

    @Override
    public ClientLifeStyle connect(InetSocketAddress address)    {
        config(address);

        try{
            connection =  client.connectNow();
        }catch (Exception exception){
            log.error("connect server {}  port {} encounter error , stack is \n {}",address.getHostString(),address.getPort(), ExceptionUtil.stacktraceToString(exception));
            throw new IllegalArgumentException("remote server is invalid!");
        }

        return this;
    }


    @Override
    public Mono<Void> sendObject(Object message) {
        if (isAlive()) {

            NettyOutbound nettyOutbound = connection.outbound().sendObject(Mono.just(message));

            return nettyOutbound.then();

        }
        throw new IllegalArgumentException("The connection is disConnect!");

    }

    @Override
    public Mono<Void> sendString(String message) {
        if (isAlive()) {

            NettyOutbound nettyOutbound = connection.outbound().sendString(Mono.just(message));

            return nettyOutbound.then();

        }
        throw new IllegalArgumentException("The connection is disConnect!");
    }

    @Override
    public ClientLifeStyle connect() throws IllegalArgumentException {
        return connect(address);
    }

    @Override
    public Boolean reTryConnect() throws IllegalArgumentException {
        Callable<Boolean> callable = () -> {
            if (isAlive()){
                return Boolean.TRUE;
            }
            ClientLifeStyle connect = connect();
            return true;
        };
        return Flux.from(
                    Mono.fromCallable(callable))
                    .retryWhen(
                        Retry
                        .backoff(3, Duration.ofSeconds(1)).jitter(0.3d)
                        .filter(throwable -> throwable instanceof IllegalArgumentException)
                        .onRetryExhaustedThrow((spec, rs) -> new IllegalArgumentException("remote server is invalid !pls retry later!")))
                .onErrorResume(throwable -> Mono.just(Boolean.FALSE))
                .blockFirst();
    }

    @Override
    public void releaseChannel() {
        connection.onDispose().subscribe();
    }

    @Override
    public Boolean isAlive() {
        return connection != null
                && !connection.isDisposed()
                && connection.channel().isActive();
    }

    private Connection ensureConnection() {
        if (!isAlive()) {
            connect(); // 确保 connect() 返回 Mono<Connection>

        }
        return connection;
    }
    @Override
    public Mono<Void> sendMessage(Message message) {
        ensureConnection();
        NettyOutbound nettyOutbound = connection.outbound().sendObject(Mono.just(message));
//        NettyOutbound nettyOutbound = connection.outbound().sendByteArray(Mono.just(message.toByteArray()));

        return nettyOutbound.then();


//        throw new IllegalArgumentException("connection is invalid !");
    }



    private enum SingleInstance{
        INSTANCE;
        private final ReactorTcpClient instance;
        SingleInstance(){
            instance = new ReactorTcpClient();
        }
        private ReactorTcpClient getInstance(){
            return instance;
        }
    }
    public static ReactorTcpClient getInstance(){
        return SingleInstance.INSTANCE.getInstance();
    }


}
