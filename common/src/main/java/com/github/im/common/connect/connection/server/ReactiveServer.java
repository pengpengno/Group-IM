package com.github.im.common.connect.connection.server;

import com.github.im.common.connect.connection.server.tcp.ReactorTcpServer;
import com.google.inject.Inject;
import java.net.InetSocketAddress;

/**
 * 流式 server
 * @author pengpeng
 * @description
 * @date 2023/2/28
 */
public interface ReactiveServer {

    @Inject
    public default ReactiveServer getInstance(ReactorTcpServer tcpServer){
        return tcpServer;
    }

    public ReactiveServer init (InetSocketAddress address);


    public ReactiveServer start();


    public void stop();




}
