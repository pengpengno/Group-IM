package com.github.im.common.reactor.flux.retry;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author pengpeng
 * @description
 * @date 2023/1/10
 */
@Slf4j
public class FluxTestCase {
    @Test
    @DisplayName("Retry 重试")
    public void error(){
        AtomicInteger errorCount = new AtomicInteger();
        Flux<String> flux =
            Flux.<String>error(new ConnectException("boom"))
                .doOnError(e -> {
                    errorCount.incrementAndGet();
                    log.info(e + " at  " + LocalTime.now() + errorCount.get());
                })
                .doAfterTerminate(()-> log.info("terminate"))
                .retryWhen(
                        Retry
                        .backoff(3, Duration.ofSeconds(1)).jitter(0.3d)
                                .filter(throwable -> throwable instanceof ConnectException)
                        .doAfterRetry(rs -> log.info("retried at " + LocalTime.now() + ", attempt " + rs.totalRetries()))
                        .onRetryExhaustedThrow((spec, rs) -> rs.failure())
                );
        StepVerifier
            .create(flux)
                .verifyError()
        ;
        Assertions.assertEquals(errorCount.get(),4);

    }




    public static void main(String[] args) {

        AtomicInteger errorCount = new AtomicInteger();
        Flux<String> flux =
                Flux.<String>error(new IllegalStateException("boom"))
                        .doOnError(e -> {
                            errorCount.incrementAndGet();
                            System.out.println(e + " at " + LocalTime.now());
                        })
                        .retryWhen(Retry
                                .fixedDelay(3, Duration.ofMillis(291))
                                .jitter(0d)  // 禁用抖动
                                .doAfterRetry(rs -> System.out.println("retried at " + LocalTime.now() + ", attempt " + rs.totalRetries()))
                                .onRetryExhaustedThrow((spec, rs) -> rs.failure())
                        );
        flux.subscribe();
    }


    @Test
    public void retryWhen3(){
        AtomicInteger errorCount = new AtomicInteger();
        Flux<String> flux =
                Flux.<String>error(new IllegalArgumentException())
                        .doOnError(e -> errorCount.incrementAndGet())
                        .retryWhen(Retry.from(companion ->
                                companion.map(rs -> {
                                    if (rs.totalRetries() < 3) {
                                        log.info("重置 {}",rs.totalRetries());
                                        return rs.totalRetries();
                                    }
                                    else throw Exceptions.propagate(rs.failure());
                                })
                        )
                        );

        flux.subscribe();
    }

}

