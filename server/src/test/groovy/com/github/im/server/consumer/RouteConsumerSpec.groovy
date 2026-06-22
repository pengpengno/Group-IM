package com.github.im.server.consumer

import com.github.im.common.connect.model.proto.BaseMessage
import com.github.im.server.service.ClusterLocalDeliveryService
import com.github.im.server.service.MessageRouter
import com.github.im.server.service.RedisMessageRouter
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.util.Base64

class RouteConsumerSpec extends Specification {

    StringRedisTemplate redisTemplate = Mock()
    StreamOperations streamOperations = Mock()
    ClusterLocalDeliveryService clusterLocalDeliveryService = Mock()

    RouteConsumer consumer = new RouteConsumer(redisTemplate, clusterLocalDeliveryService)

    def setup() {
        redisTemplate.opsForStream() >> streamOperations
    }

    def "should decode binary route message and acknowledge it"() {
        given:
        def payload = sampleMessage("hello")
        def recordId = RecordId.of("1-0")
        def record = Stub(MapRecord) {
            getId() >> recordId
            getValue() >> [
                    (RedisMessageRouter.FIELD_TO)          : "6",
                    (RedisMessageRouter.FIELD_BODY)        : Base64.encoder.encodeToString(payload.toByteArray()),
                    (RedisMessageRouter.FIELD_PAYLOAD_KIND): RedisMessageRouter.PAYLOAD_KIND_IM_BINARY
            ]
        }

        when:
        invokePrivate(consumer, "onMessage", record)

        then:
        1 * clusterLocalDeliveryService.deliverBaseMessage(6L, {
            it.toByteArray() == payload.toByteArray()
        })
        1 * streamOperations.acknowledge({
            it == "${MessageRouter.STREAM_ROUTE_PREFIX}${com.github.im.server.config.NodeId.NODE_ID}"
        }, MessageRouter.CONSUMER_GROUP, recordId)
    }

    def "should decode text route message and acknowledge it"() {
        given:
        def payload = '{"type":"offer"}'
        def recordId = RecordId.of("2-0")
        def record = Stub(MapRecord) {
            getId() >> recordId
            getValue() >> [
                    (RedisMessageRouter.FIELD_TO)          : "6",
                    (RedisMessageRouter.FIELD_BODY)        : Base64.encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8)),
                    (RedisMessageRouter.FIELD_PAYLOAD_KIND): RedisMessageRouter.PAYLOAD_KIND_SIGNAL_TEXT
            ]
        }

        when:
        invokePrivate(consumer, "onMessage", record)

        then:
        1 * clusterLocalDeliveryService.deliverSignalText(6L, payload)
        1 * streamOperations.acknowledge({
            it == "${MessageRouter.STREAM_ROUTE_PREFIX}${com.github.im.server.config.NodeId.NODE_ID}"
        }, MessageRouter.CONSUMER_GROUP, recordId)
    }

    def "should acknowledge invalid route records so they do not block the stream"() {
        given:
        def recordId = RecordId.of("3-0")
        def record = Stub(MapRecord) {
            getId() >> recordId
            getValue() >> [:]
        }

        when:
        invokePrivate(consumer, "onMessage", record)

        then:
        0 * clusterLocalDeliveryService._
        1 * streamOperations.acknowledge({
            it == "${MessageRouter.STREAM_ROUTE_PREFIX}${com.github.im.server.config.NodeId.NODE_ID}"
        }, MessageRouter.CONSUMER_GROUP, recordId)
    }

    private static BaseMessage.BaseMessagePkg sampleMessage(String content) {
        BaseMessage.BaseMessagePkg.newBuilder()
                .setMessage(com.github.im.common.connect.model.proto.Chat.ChatMessage.newBuilder()
                        .setConversationId(1L)
                        .setClientMsgId("client-${content}")
                        .setContent(content)
                        .setType(com.github.im.common.connect.model.proto.Chat.MessageType.TEXT)
                        .setFromUser(com.github.im.common.connect.model.proto.User.UserInfo.newBuilder().setUserId(7L).build())
                        .build())
                .build()
    }

    private static Object invokePrivate(Object target, String methodName, Object arg) {
        def method = target.class.getDeclaredMethod(methodName, MapRecord)
        method.accessible = true
        method.invoke(target, arg)
    }
}
