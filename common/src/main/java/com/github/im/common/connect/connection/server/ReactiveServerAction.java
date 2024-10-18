package com.github.im.common.connect.connection.server;

import com.github.im.common.connect.connection.server.context.IConnection;
import com.google.protobuf.Message;
import reactor.core.publisher.Mono;

import java.net.ConnectException;

/**
 * 服务端发送消息至客户端
 * @author pengpeng
 * @description
 * @date 2023/3/7
 */
public interface ReactiveServerAction {


    /**
     * 发送消息
     * @param connection 连接
     * @param message 消息
     * @return Mono<Void>
     * @throws ConnectException 连接异常
     */
    Mono<Void>  sendString(IConnection connection, String message) throws ConnectException;

    /***
     * <pre>
     *     account 是用于在 server 端 获取对应connection 的key
     *     {@code IConnection iConnection = contextAction.applyConnection(account);}
     *
     * </pre>
     * @param account 用以客户端绑定 对应connection 的 标识
     * @param message 需要发送的消息
     * @return
     * @throws ConnectException
     */
    Mono<Void>  sendString(String account,String  message) throws ConnectException;


    Mono<Void>  sendMessage(Message message) throws ConnectException;

//    Mono<Void>





    /***
     *
     * @param account
     * @param message
     * @return
     */
    Mono<Void>  sendMessage(String account , Message message) throws ConnectException;









}
