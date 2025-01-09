package com.github.im.common.connect.connection.server.context;

import cn.hutool.core.collection.CollUtil;
import io.netty.buffer.ByteBuf;
import lombok.Builder;
import lombok.Data;
import reactor.core.publisher.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/***
 * 链接分组
 */

@Data
@Builder
public class ConnectionGroupRoom {

    private String roomKey ;

    private List<IConnection> connections ;


    Queue<byte[]> data ; // 用于存储数据

    private IConnection connection ;

    public volatile Queue<ByteBuf> byteBufQueue;

    public final Object object = new Object();

    private ByteBuf lastByteBuf ;

    private Flux<byte[]> byteFlux ;

    public void offerByteBuf (ByteBuf byteBuf){
        if (byteBufQueue == null){
            synchronized (object){
                if (byteBufQueue == null){
                    byteBufQueue = new ConcurrentLinkedQueue<>();
                }
            }
        }
        boolean offer = byteBufQueue.offer(byteBuf);
//        TODO  not thread safety
        if (!offer){
            lastByteBuf = byteBufQueue.poll();

            byteBufQueue.offer(byteBuf);

        }
    }


    public ByteBuf pollByteBuf () {

        if (CollUtil.isNotEmpty(byteBufQueue)){
            lastByteBuf = byteBufQueue.poll();
            return lastByteBuf;
        }

        return lastByteBuf;
    }
    public void addConnection(IConnection connection){

        Mono.create(sink -> {

            sink.success(connection);

        }).subscribe();


        if (connections == null){
            synchronized (object){
                if (connections == null){
                    connections = new ArrayList<>();
                }
            }
        }
        connections.add(connection);

    }

    ReplayProcessor<byte[]> objectReplayProcessor = ReplayProcessor.create(50);
    public void replayCache (){
        byteFlux.subscribe(objectReplayProcessor);
//        ConnectableFlux<byte[]> replay = byteFlux.replay(50);

    }

    public Flux<byte[]>  lastCache (){
        return objectReplayProcessor;
    }



    public byte[] offerFrameData () {

        return byteFlux.blockFirst();
//        if (data == null){
//            return null;
//        }
//        return data.poll();
    }



}
