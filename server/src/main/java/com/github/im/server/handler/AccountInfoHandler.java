//package com.github.im.server.handler;
//
//import com.github.im.common.connect.connection.server.ProtoBufProcessHandler;
//import com.github.im.common.connect.connection.server.ServerToolkit;
//import com.github.im.common.connect.connection.server.context.IConnectContextAction;
//import com.github.im.common.connect.enums.ProtocolMessageMapEnum;
//import com.github.im.common.connect.model.proto.Account;
//import com.google.protobuf.Message;
//import org.springframework.stereotype.Component;
//import reactor.netty.Connection;
//
///**
// * chat message process
// */
//@Component
//public class AccountInfoHandler implements MessageH {
//
//    @Override
//    public ProtocolMessageMapEnum type() {
//        return ProtocolMessageMapEnum.CHAT;
//    }
//
//    @Override
//    public void process(Connection con, Message message) {
//
//            Account.AccountInfo accountInfo = (Account.AccountInfo) message;
//
////            Any chat = chatMessage.getChat();
//
//            IConnectContextAction contextAction = ServerToolkit.contextAction();
//
//    }
//}
