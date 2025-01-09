//package com.github.im.common.connect.connection.server;
//
//import cn.hutool.core.collection.CollectionUtil;
//import com.github.im.common.connect.model.proto.BaseMessage;
//import com.github.im.common.connect.utils.ByteBufUtils;
//import com.google.protobuf.Message;
//import io.netty.buffer.ByteBuf;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.BeansException;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.ApplicationContextAware;
//import reactor.netty.Connection;
//import java.util.Map;
//import java.util.stream.Collectors;
//
///**
// * message process
// * @author pengpeng
// * @description 处理
// * @date 2023/3/16
// */
//@Slf4j
//public class ByteBufProcessService implements ApplicationContextAware ,ByteBufProcess {
//
//    public Map<Class<? extends Message> , ProtoBufProcessHandler> processMap;
//
//    @Override
//    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
//
//        Map<String, ProtoBufProcessHandler> protoBufProcessMap = applicationContext.getBeansOfType(ProtoBufProcessHandler.class);
//
//        if (CollectionUtil.isNotEmpty(protoBufProcessMap)){
//            processMap =  protoBufProcessMap
//                        .values()
//                        .stream()
//                        .filter(e->e.type()!=null)
//                        .collect(Collectors.toMap(e -> e.type()
//                                .getMessageClass(), e ->e, (e1, e2) -> e1));
//        }
//
//    }
//
//
//
//    @Override
//    @SuppressWarnings("unchecked")
//    public void process(Connection con, ByteBuf byteBuf)  {
//
//        var bytes = ByteBufUtils.readByteBuf2Array(byteBuf);
//        try{
//
//            var baseMessagePkg = BaseMessage.BaseMessagePkg.parseFrom(bytes);
//            var payloadCase = baseMessagePkg.getPayloadCase();
//            switch (payloadCase){
//                case ACCOUNTINFO -> {
//                    var accountInfo = baseMessagePkg.getAccountInfo();
//                    if (processMap.containsKey(accountInfo.getClass())){
//                        processMap.get(accountInfo.getClass()).process(con,accountInfo);
//                    }
//                }
//            }
//
//        }catch (Exception ex){
//            log.error("Illegal message ",ex);
//        }
//
////        Message message = ByteBufUtils.readByteBuf2Message(byteBuf);
//
////        Parser<? extends Message> parserForType = message.getParserForType();
//
////        Message message = ByteBufUtils.readByteBuf2Message(byteBuf);
//    }
//
//
//    private enum SingleInstance{
//        INSTANCE;
//        private final ByteBufProcessService instance;
//        SingleInstance(){
//            instance = new ByteBufProcessService();
//        }
//        private ByteBufProcessService getInstance(){
//            return instance;
//        }
//    }
//    public static ByteBufProcessService getInstance(){
//        return SingleInstance.INSTANCE.getInstance();
//    }
//    private ByteBufProcessService(){}
//
//
//
//
//}
