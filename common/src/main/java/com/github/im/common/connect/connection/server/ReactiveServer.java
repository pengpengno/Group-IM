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

    /**
     * 初始化 服务 配置信息
     * @param address
     * @return
     */
    public ReactiveServer init (InetSocketAddress address);


    /**
     * 启动服务
     * @return
     */
    public ReactiveServer start();


    default public boolean isRunning(){
        return true ;
    }

    public void stop();




}
