package com.github.im.common.connect.connection;

import com.github.im.common.connect.connection.server.BindAttr;
import com.github.im.common.connect.enums.PlatformType;
import com.github.im.common.connect.model.proto.BaseMessage;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Slf4j
public class ReactiveConnectionManager {

//    private static final ConcurrentMap<String, Sinks.Many<Chat.ChatMessage>> chatMessageSinks = new java.util.concurrent.ConcurrentHashMap<>();

    private static final ConcurrentMap<BindAttr<String>, Sinks.Many<BaseMessage.BaseMessagePkg>> BASE_MESSAGE_SINKS
            = new java.util.concurrent.ConcurrentHashMap<>();

    public static final String ALL_PLATFORM_PUSH_TAG = "ALL";

    /**
     * 注册一个Sink流程用于处理特定属性的消息
     * 此方法旨在确保每个属性都有一个对应的Sink实例来处理消息，
     * 如果该ATTR尚未关联任何Sink，则创建一个新的Sink实例并关联
     *
     * ATTR 的创建可以 参考  {@link BindAttr#getBindAttr(String, PlatformType)}  {@link BindAttr#getBindAttr(User.UserInfo)}
     *
     * @param ATTR 属性名称，用作在BASE_MESSAGE_SINKS映射中的键
     */
    public static Sinks.Many<BaseMessage.BaseMessagePkg> registerSinkFlow(BindAttr<String> ATTR) {
        // 使用putIfAbsent方法来确保在BASE_MESSAGE_SINKS映射中不存在该属性时，
        // 创建一个新的Sink实例并放入映射中，这避免了重复创建Sink实例

        var containsKey = BASE_MESSAGE_SINKS.containsKey(ATTR);

        if (containsKey) {
            return BASE_MESSAGE_SINKS.get(ATTR);
        }

        Sinks.Many<BaseMessage.BaseMessagePkg> sinkFlow = Sinks.many().multicast().onBackpressureBuffer();
        BASE_MESSAGE_SINKS.putIfAbsent(ATTR, sinkFlow);
        return sinkFlow;
    }

    /**
     * 获取已经注册的Sink流程
     * 此方法用于获取已经注册的Sink实例
     *
     * @param ATTR 属性名称，用作在BASE_MESSAGE_SINKS映射中的键
     * @return 已注册的Sink实例，如果不存在则返回null
     */
    public static Sinks.Many<BaseMessage.BaseMessagePkg> getSinkFlow(BindAttr<String> ATTR) {
        return BASE_MESSAGE_SINKS.get(ATTR);
    }


    /**
     * 判断是否 存在了 sink
     * @param ATTR {@link BindAttr#getBindAttr(User.UserInfo)}
     * @return 存在返回true  不存在返回false
     */
    public static boolean isSubscribe(BindAttr<String> ATTR) {
//        return BASE_MESSAGE_SINKS.containsKey(ATTR);
        return !getAllSubscribeAttr(ATTR).isEmpty();
    }



    /**
     * 添加一个BaseMessage
     * 如果 ATTR 不存再 那么就 直接返回  静默忽略即可
     * @param ATTR
     * @param baseMessagePkg
     */
    public static void addBaseMessage(BindAttr<String> ATTR , BaseMessage.BaseMessagePkg baseMessagePkg) {
        if(isSubscribe(ATTR))
        {
            /**如果在线就推送**/
            getAllSubscribeAttr(ATTR).forEach(
                    bindAttr -> {
                        log.info("推送消息 {} 到 {} ",baseMessagePkg,bindAttr.getKey());
                        BASE_MESSAGE_SINKS.get(bindAttr).tryEmitNext(baseMessagePkg).orThrow();
                    }
            );
//            BASE_MESSAGE_SINKS.get(ATTR).tryEmitNext(baseMessagePkg).orThrow();

        }else{
            /**不在线 不操作**/
            log.debug("未找到对应的 sink , ATTR : {}",ATTR);
        }

    }

    /**
     * 获取当前bind key 下所有在线 的绑定key
     * @param ATTR
     * @return
     */
    public static List<BindAttr<String>> getAllSubscribeAttr(BindAttr<String> ATTR) {
        if (ATTR != null){
            var key = ATTR.getKey();
            if (key != null){
                if (key.contains(ALL_PLATFORM_PUSH_TAG)){
                    // 如果是推送全平台的标识
                    log.debug("推送全平台的标识");
                    var i = key.lastIndexOf("_");
                    var account = key.substring(0,i);
                    return Arrays.stream(PlatformType.values()).map(
                            platformType -> {
                                var bindAttr = BindAttr.getBindAttr(account, platformType);
                                var PLATFORM_IS_ONLINE = BASE_MESSAGE_SINKS.containsKey(bindAttr);
                                if (PLATFORM_IS_ONLINE){
                                    return bindAttr;
                                }
                                return null;
                            }
                    ).filter(Objects::nonNull).collect(Collectors.toList());

                }else{
                    if (BASE_MESSAGE_SINKS.containsKey(ATTR)){
                        return List.of(ATTR);
                    }
                }
            }
        }
        return emptyList();
    }


    /**
     * 取消订阅
     */
    public static void unSubscribe(BindAttr<String> ATTR) {

        if (ATTR == null){
            return;
        }
        try{

            var subscribe = isSubscribe(ATTR);
            if(subscribe){

                BASE_MESSAGE_SINKS.get(ATTR).tryEmitComplete();
                BASE_MESSAGE_SINKS.remove(ATTR);
            }

        }catch (Exception e){
            log.error("unSubscribe error",e);
        }

    }
}
