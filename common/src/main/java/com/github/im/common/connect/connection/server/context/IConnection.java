package com.github.im.common.connect.connection.server.context;

import com.github.im.common.connect.enums.ConnectionStatus;
import com.github.im.common.connect.model.proto.Account;
import io.netty.channel.Channel;
import reactor.netty.Connection;


/**
 * 服务端使用的 connection 抽象接口
 * @author pengpeng
 * @description
 * @date 2023/3/3
 */
public interface IConnection {

//    @NotNull(message = "connection could not be null , when put new connection in cache",groups = Create.class)
    public Channel channel();

//    @NotNull(message = "connection could not be null",groups = Create.class)
    public Connection connection();

//    @NotNull(message = "accountInfo could not be null",groups = Create.class)
    public Account.AccountInfo accountInfo();

    public ConnectionStatus status();

    public Boolean online();

    public void close();

    /**
     * group
     * @return
     */
    public String group ();



    public static interface Create{

    }






}
