package com.github.im.common.connect.handler.proto;

import com.github.im.common.connect.module.ConnectionModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.protobuf.MessageLite;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;

import java.util.Collection;
import java.util.stream.Collectors;

public class ProtoBufMessageLiteScanner {

    private static Injector injector = null ;

    private static Collection<ProtobufDecoder> protobufDecoders = null;

    private final static Object lock = new Object();
    private final static Object lock2 = new Object();

    public static Injector getInjector() {
        if (injector == null){
            synchronized (lock){
                if (injector == null){
                    injector = Guice.createInjector( new ConnectionModule());
                }
            }
        }
        return injector;

    }



    /**
     * 扫描所有的 messageLite
     * @return
     */
    public static Collection<? extends MessageLite> scanAllMessageLite(){
        return getInjector().findBindingsByType(TypeLiteral.get(MessageLite.class))
                .stream().map(e-> e.getProvider().get().getDefaultInstanceForType())
                .toList();
    }


    public static ProtobufEncoder  protobufEncoder(){

        return getInjector().getInstance(ProtobufEncoder.class);

    }

    public static Collection<ProtobufDecoder> protobufDecoders(){
        if (protobufDecoders == null){
            synchronized (lock2){
                if (protobufDecoders == null){
                    return scanAllMessageLite().stream()
                        .map(ProtobufDecoder::new).collect(Collectors.toList());
                }
            }
        }
        return protobufDecoders;
    }
}
