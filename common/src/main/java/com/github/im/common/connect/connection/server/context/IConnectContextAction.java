package com.github.im.common.connect.connection.server.context;

import java.net.ConnectException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *
 * connection 容器
 * @author pengpeng
 * @description
 * @date 2023/3/6
 */
public interface IConnectContextAction {



    /***
     * @param bindKey 绑定key
     * @return  返回指定账户的 connection 如果不存在则返回空
     */
    public IConnection applyConnection(String bindKey) ;

    public void putConnection(IConnection connection);


    public ConnectionGroupRoom getOrSupplier (String key ,  Function<String, Supplier<ConnectionGroupRoom>> connectionFactor);


    public ConnectionGroupRoom applyConnectionGroup(String roomKey);

    /***
     * 关闭指定账户的连接
     * @param bindKey  attr bind to channel
     */
    public void closeAndRmConnection(String bindKey) throws ConnectException;





}
