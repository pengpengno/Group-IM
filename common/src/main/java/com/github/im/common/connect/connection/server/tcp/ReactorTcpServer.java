package com.github.im.common.connect.connection.server.tcp;

import com.github.im.common.connect.spi.ReactiveHandlerSPI;
import com.github.im.common.connect.connection.server.ReactiveServer;
import com.github.im.common.connect.connection.ConnectionConstants;
import com.github.im.common.util.RtspServer;
import com.google.inject.Singleton;
import io.netty.handler.codec.rtsp.RtspEncoder;
import io.netty.handler.logging.LogLevel;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpServer;

import java.net.InetSocketAddress;

/**
 * 响应式 tcp 链接
 * @author pengpeng
 * @description
 * @date 2023/3/3
 */
@Slf4j
@Singleton
public class ReactorTcpServer implements ReactiveServer {


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

    private ReactorTcpServer(){
    }

    @Override
    public void stop() {
        disposableServer.disposeNow();
    }


    public ReactiveServer init(InetSocketAddress address){
        this.address = address;

        server = TcpServer
                .create()
                .wiretap("tcp-server", LogLevel.INFO)
                .port(address.getPort())
                .doOnConnection(connection -> {
//                    ConnectionConsumer.getInstance(connection).accept(connection);
                    connection.channel().attr(ConnectionConstants.ROOM_KEY).set("group");

                    connection
//                            .addHandlerLast(new RtspDecoder())
//                            .addHandlerLast(new RtspServer.RtspServerHandler())
//                            .addHandlerLast(new ServerInboundHandler())
                            .addHandlerLast(new RtspEncoder())
//                            .addHandlerLast(new RtspDecoder())
                            ;
                })
                //  注入 执行的handler
                .handle(ReactiveHandlerSPI.wiredSpiHandler().handler())
        ;

        log.info("startup netty  on port {}",address.getPort());

        return this;
    }


//    public ReactiveServer getInstance(InetSocketAddress address){
//
//        DisposableServer server =
//                HttpServer.create()
//                        .port(8080)
//                        .route(routes -> {
//                            // 添加HTTP请求处理器
//                            routes.get("/hello", (req, res) ->
//                                    res.status(HttpResponseStatus.OK)
//                                    .sendString(Mono.just("Hello World!")));
//
//                            // 添加WebSocket处理器
//                            routes.ws("/ws", (inbound, outbound) -> {
//                                // 处理WebSocket消息
//                                return outbound.sendString(inbound.receive()
//                                        .retain()
//                                        .map(byteBuf -> {
//                                            String msg = byteBuf.toString(io.netty.util.CharsetUtil.UTF_8);
//                                            return "Received WebSocket message: " + msg;
//                                        }));
//                            });
//                        })
//                        .bindNow();
//
//    }

    public ReactiveServer start(){
        if(address == null || address.isUnresolved()){
            log.error("start server error ");
        }

        log.info("start netty server on port {}",address.getPort());
        disposableServer = server.bindNow();
        disposableServer.onDispose().block();
        return this;
    }



}
