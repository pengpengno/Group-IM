package com.github.im.common.connect.connection.server.context;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.github.im.common.connect.model.proto.Account;
import com.github.im.common.connect.model.proto.OnLineUser;
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

    /**
     * 获取指定集合中的在线用户
     * @param account
     * @return
     */
    public Set<String> filterOnlineAccount(Set<String> account){
        return filterOnlineIConnection(account).stream().map(e-> e.accountInfo().getAccount()).collect(Collectors.toSet());
    }


    @SneakyThrows
    public OnLineUser.UserSearch filterOnlineByUserSearch(OnLineUser.UserSearch userSearch){
        if (userSearch != null ){
            List<Account.AccountInfo> accountsList = userSearch.getAccountsList();
            if (CollectionUtil.isNotEmpty(accountsList)){
                List<Account.AccountInfo> accountInfos = accountsList.stream()
                        .filter(l -> null != l && StrUtil.isNotBlank(l.getAccount()))
                        .filter(e -> {
                            String account = e.getAccount();
                            IConnection connection = contextAction.applyConnection(account);
                            if (connection == null ){
                                log.info(" account :{} is offline !",account);
                            }
                            return connectionValid(connection);
                        })
                        .collect(Collectors.toList());
                return OnLineUser.UserSearch.newBuilder().addAllAccounts(accountInfos).build();
            }
        }

        return OnLineUser.UserSearch.newBuilder().build();
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
