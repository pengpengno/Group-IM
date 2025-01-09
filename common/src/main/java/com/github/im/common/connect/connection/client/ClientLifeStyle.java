package com.github.im.common.connect.connection.client;

import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.time.Duration;

/**
 * manage client config  connect stop and offline-re-Try
 */
public interface ClientLifeStyle {



    public ClientLifeStyle config(InetSocketAddress address) ;
    /***
     * 连接远程节点
     * @param address
     * @return
     */
    public ClientLifeStyle connect (InetSocketAddress address);

    /**
     * 开启channel 通道连接
     *
     * @return
     */
    public ClientLifeStyle connect() throws ConnectException;


    /***
     * 重试连接 如果当前连接 处于正常状态则直接返回
     * @return connect state
     */
    public  default Boolean reTryConnect() throws ConnectException {
        if (isAlive()){
            return Boolean.TRUE;
        }

        Flux<ClientLifeStyle> flux =
        Flux.just(connect())
            .retryWhen(
            Retry
            .backoff(3, Duration.ofSeconds(1)).jitter(0.3d)
            .filter(throwable -> throwable instanceof Exceptions.SourceException)
            .onRetryExhaustedThrow((spec, rs) -> new ConnectException("remote server is invalid !")));
        flux.subscribe();
        return Boolean.TRUE;
    }




    /**
     * 释放 channel 断线资源
     */
    public void releaseChannel();

    /**
     * channel 是否存活
     * @return
     */
    public Boolean isAlive();
}
