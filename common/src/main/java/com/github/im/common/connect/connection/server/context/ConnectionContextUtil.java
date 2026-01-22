package com.github.im.common.connect.connection.server.context;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.github.im.common.connect.connection.server.ServerToolkit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author pengpeng
 * @description
 * @date 2023/3/22
 */
@Slf4j
public class ConnectionContextUtil {

//
    private IConnectContextAction contextAction;

    private ConnectionContextUtil(){
        contextAction = ServerToolkit.contextAction();
    }


    private enum SingleInstance{
        INSTANCE;
        private final ConnectionContextUtil instance;
        SingleInstance(){
            instance = new ConnectionContextUtil();
        }
        private ConnectionContextUtil getInstance(){
            return instance;
        }
    }
    public static ConnectionContextUtil getInstance(){
        return SingleInstance.INSTANCE.getInstance();
    }


    public Boolean connectionValid(IConnection connection){
        return null != connection && connection.online();
    }

    /**
     * 获取指定集合中的在线用户
     * @param account
     * @return
     */
    public Set<IConnection> filterOnlineIConnection(Set<String> account){
        return account.stream()
                .map(e-> contextAction.applyConnection(e))
                .filter(con-> connectionValid(con)).collect(Collectors.toSet());
    }



}
