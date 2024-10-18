package com.github.im.server.handler;//package com.github.meeting.server.handler;
//
//import cn.hutool.core.util.StrUtil;
//import com.github.meeting.common.connect.connection.server.ProtoBufProcess;
//import com.github.meeting.common.connect.connection.server.context.IConnectContextAction;
//import com.github.meeting.common.connect.connection.server.context.ReactorConnection;
//import com.github.meeting.common.connect.connection.ConnectionConstants;
//import com.github.meeting.common.connect.enums.ProtocolMessageMapEnum;
//import com.github.meeting.common.connect.model.proto.Account;
//import com.google.protobuf.Message;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.lang.Nullable;
//import org.springframework.stereotype.Component;
//import reactor.core.publisher.Mono;
//import reactor.netty.Connection;
//@Component
//@Slf4j
//public class AuthProcessService implements ProtoBufProcess {
//
//
//    @Autowired
//    IConnectContextAction contextAction;
//
//    @Override
//    public ProtocolMessageMapEnum type() {
//        return ProtocolMessageMapEnum.AUTH;
//    }
//
//    @Override
//    public void process(@Nullable Connection con, Message message) {
//
//        if (null != message && con != null){
//
//            Account.Authenticate auth = (Account.Authenticate) message;
//
//            Account.AccountInfo accountInfo = auth.getAccountInfo();
//
//            if (con.channel().isActive() && StrUtil.isNotBlank(accountInfo.getAccount())){
//
//                con.channel().attr(ConnectionConstants.BING_ACCOUNT_KEY).set(accountInfo);
//
//                ReactorConnection connection = ReactorConnection.builder()
//                        .channel(con.channel())
//                        .connection(con)
//                        .accountInfo(accountInfo)
//                        .build();
//
//                contextAction.putConnection(connection);
//
//                log.info("account :  {} is binding  !",accountInfo.getAccount());
//
//                con.outbound().sendString(Mono.justOrEmpty("Reactor Netty Channel  has been established , network transport now is available！")).then().subscribe();
//
//            }
//
//            return;
//        }
//
//        throw  new IllegalArgumentException("The connection and message is invalid!");
//
//
//    }
//}
