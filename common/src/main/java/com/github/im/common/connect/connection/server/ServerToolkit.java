package com.github.im.common.connect.connection.server;


import com.github.im.common.connect.connection.server.context.IConnectContext;
//import com.github.im.common.connect.connection.server.tcp.ReactorTcpAction;
import com.github.im.common.connect.connection.server.tcp.ReactorTcpServer;
import com.github.im.common.connect.connection.server.context.IConnectContextAction;

/**
 * server 工具包
 * 用以获取 server 端 如下 实体
 * <ul>
 *     <li>服务端 与 客户端 建立的 连接{@link reactor.netty.Connection}</li>
 *     <li>服务端 Server 实体 {@link ReactiveServerAction }</li>
 * </ul>
 *
 * @author pengpeng
 * @description
 * @date 2023/3/6
 */
public class ServerToolkit {


    /**
     * 获取服务端的 {@link  reactor.netty.Connection}容器
     * @return
     */
    public static IConnectContextAction contextAction(){
        return IConnectContext.getInstance();
    }

//    public static ReactiveServerAction reactorTcpServerAction(){
//        return ReactorTcpAction.getInstance();
//    }


    public static ReactiveServer reactiveServer(){
        return ReactorTcpServer.getInstance();
    }

}
