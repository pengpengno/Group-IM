package com.github.im.common.reactor.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.*;
import reactor.test.StepVerifier;

/**
 * @author pengpeng
 * @description
 * @date 2023/3/17
 */
@Slf4j
public class ReactorRabbit {

    private final static String host  = "localhost";
    private final static Integer port  = 5672;

    private final static String username = "admin";
    private final static  String password  = "admin";
    private final static  String virtualHost  = "/";

    /**
     * 使用RabbitFlux发送消息的测试方法。
     * 该方法配置了一个发送者，创建了一个消息流，并向RabbitMQ发送了该消息流中的所有消息。
     * 消息发送过程包括：声明交换器、声明队列、绑定队列和交换器，然后发送消息。
     *
     * @throws InterruptedException 如果线程在执行过程中被中断
     */
    @Test
    @DisplayName("reactor-rabbitmq-发送消息")
    public void send() throws InterruptedException {

        final String exchangeName = "reactive.rabbit";
        final String queueName = "reactive.message";
        final String routeKey = "chat.message";

        // 配置发送者选项，包括连接工厂和资源管理调度器
        SenderOptions senderOptions = new SenderOptions()
                .connectionFactory(createFactory())
                .resourceManagementScheduler(Schedulers.boundedElastic());
        // 基于配置创建发送者
        Sender sender = RabbitFlux.createSender(senderOptions);

        // 创建一个消息流，包含10条消息
        Flux<OutboundMessage> map = Flux.range(0, 10)
                .map(i -> new OutboundMessage(exchangeName, routeKey, ("sent tot you " + i).getBytes()));

        // 声明交换器
        Mono<AMQP.Exchange.DeclareOk> exchange = sender.declareExchange(ExchangeSpecification.exchange(exchangeName));
        exchange.subscribe();

        // 声明队列
        Mono<AMQP.Queue.DeclareOk> queue = sender.declareQueue(QueueSpecification.queue(queueName));
        queue.subscribe();

        // 绑定队列和交换器
        Mono<AMQP.Queue.BindOk> bind = sender.bind(BindingSpecification.queueBinding(exchangeName, "chat.message", "receive.message"));
        bind.subscribe();

        // 发送消息并验证发送完成
        StepVerifier.create(sender.send(map.doOnNext(msg -> log.info("sent message: " + msg.toString())))
                .doOnError(e-> log.info(e.getMessage())))
                .verifyComplete();

        sender.close();
    }



    /**
     * 测试函数，用于创建一个RabbitFlux消息接收器，并从指定队列接收消息。
     * 该函数将消息手动确认机制用于处理接收到的消息。
     *
     * @throws InterruptedException 如果线程在睡眠时被中断
     */
    @Test
    public void receiver () throws InterruptedException {

        final String exchangeName = "reactive.rabbit";
        // 创建接收器选项，并配置连接工厂和订阅调度器
        ReceiverOptions receiverOptions = new ReceiverOptions()
                .connectionFactory(createFactory())
                .connectionSubscriptionScheduler(Schedulers.boundedElastic());

        // 根据选项创建消息接收器
        Receiver receiver = RabbitFlux.createReceiver(receiverOptions);

        // 从队列"receive.message"手动消费消息
        Flux<AcknowledgableDelivery> deliveryFlux = receiver.consumeManualAck(exchangeName);

        // 对消费到的每条消息进行处理，并手动确认消息
        Flux<AcknowledgableDelivery> acknowledgableDeliveryFlux = deliveryFlux.doOnNext(ack -> {
            // 将消息体转换为字符串，并记录日志
            String s = new String(ack.getBody());
            log.info("the receive message is {}", s);
            // 对消息进行确认
            ack.ack();
        });
        // 订阅消息流，开始处理消息
        StepVerifier.create(acknowledgableDeliveryFlux.then())
                .verifyComplete()
                ;
        receiver.close();


    }



    public static ConnectionFactory createFactory(){
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(virtualHost);
        connectionFactory.useNio();
        return connectionFactory;
    }
}
