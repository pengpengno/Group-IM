package com.github.im.group.gui.connect.handler;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Flux;
/**
 * Description:
 *
 * 所有由 Server 发送来的 消息通过 EventBus 接收
 * <ul>
 *     <li>{@link com.github.im.common.connect.model.proto.Chat}</li>
 * </ul>
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/1/16
 */
@Component
public class EventBus {

    private final Sinks.Many<Object> sink = Sinks.many().multicast().onBackpressureBuffer();

    private EventBus() {

    }



    /**
     * Publishes an event to the bus.
     */
    public void publish(Object event) {
        sink.tryEmitNext(event);
    }

    /**
     * Returns a Flux for subscribing to events.
     */
    public Flux<Object> asFlux() {
        return sink.asFlux();
    }
}
