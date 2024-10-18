package com.github.im.common.connect.connection.server;

import cn.hutool.core.collection.CollectionUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.im.common.connect.connection.ConnectionConstants;
import com.github.im.common.connect.connection.server.context.ConnectionGroupRoom;
import com.github.im.common.connect.connection.server.context.IConnectContextAction;
import com.github.im.common.connect.connection.server.context.ReactorConnection;
import com.google.protobuf.Message;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.rtsp.RtspDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import reactor.netty.Connection;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Data frame save
 * @author pengpeng
 * @description 根据网络传输过来的 Message 处理指定的业务
 * @date 2023/3/16
 */
@Slf4j
public class DataFrameProcessService implements ApplicationContextAware ,ByteBufProcess {

    public Map<Class<? extends Message> , ProtoBufProcess> processMap;

    private final Cache<String, Connection> connectionCache = Caffeine.newBuilder()
            .build();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        Map<String, ProtoBufProcess> protoBufProcessMap = applicationContext.getBeansOfType(ProtoBufProcess.class);

        if (CollectionUtil.isNotEmpty(protoBufProcessMap)){
            processMap =  protoBufProcessMap
                        .values()
                        .stream()
                        .filter(e->e.type()!=null)
                        .collect(Collectors.toMap(e -> e.type()
                            .getMessageClass(), e ->e, (e1, e2) -> e1));
        }

    }




    @Override
    public void process(Connection con, ByteBuf byteBuf)  {
        String roomKey = con.channel().attr(ConnectionConstants.ROOM_KEY).get();

        IConnectContextAction connectContext = ServerToolkit.contextAction();
//        con.inbound().receive().asString().
        con.addHandlerLast(new RtspDecoder());

        ConnectionGroupRoom orSupplier = connectContext.getOrSupplier(roomKey, e -> () -> {
//            HttpObjectAggregator httpObjectAggregator = new HttpObjectAggregator(1024 * 1024 * 10);

//            String s = new String(ByteBufUtils.readByteBuf2Array(byteBuf), Charset.defaultCharset());
//            HttpServerExpectContinueHandler

            ReactorConnection reactorConnection = ReactorConnection.builder()
                    .group(roomKey)
                    .connection(con)
                    .channel(con.channel())
                    .build();

            ConnectionGroupRoom connectionGroupRoom = ConnectionGroupRoom.builder()
                    .roomKey(roomKey)
                    .connection(reactorConnection)
                    .build();
            return connectionGroupRoom;

        });

        orSupplier.offerByteBuf(byteBuf);
//
//        byte[] bytes = ByteBufUtils.readByteBuf2Array(byteBuf);


    }



    private enum SingleInstance{
        INSTANCE;
        private final DataFrameProcessService instance;
        SingleInstance(){
            instance = new DataFrameProcessService();
        }
        private DataFrameProcessService getInstance(){
            return instance;
        }
    }
    public static DataFrameProcessService getInstance(){
        return SingleInstance.INSTANCE.getInstance();
    }
    private DataFrameProcessService(){}




}
