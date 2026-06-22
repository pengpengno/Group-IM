package com.github.im.server.service

import com.github.im.common.connect.model.proto.BaseMessage
import com.github.im.server.config.NodeId
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.util.Base64

class RedisMessageRouterSpec extends Specification {

    StringRedisTemplate redisTemplate = Mock()
    StreamOperations streamOperations = Mock()
    OnlineService onlineService = Mock()
    ClusterLocalDeliveryService clusterLocalDeliveryService = Mock()

    RedisMessageRouter router = new RedisMessageRouter(redisTemplate, onlineService, clusterLocalDeliveryService)

    def setup() {
        redisTemplate.opsForStream() >> streamOperations
    }

    def "should deliver IM payload locally when target user is on current node"() {
        given:
        def payload = sampleMessage("local")
        onlineService.getUserNodeId(6L) >> NodeId.NODE_ID

        when:
        router.send(7L, 6L, payload)

        then:
        1 * clusterLocalDeliveryService.deliverBaseMessage(6L, payload)
        0 * streamOperations.add(_, _)
    }

    def "should publish IM payload to target node stream when user is remote"() {
        given:
        def payload = sampleMessage("remote")
        onlineService.getUserNodeId(6L) >> "remote-node"

        when:
        router.send(7L, 6L, payload)

        then:
        1 * streamOperations.add(
                "${RedisMessageRouter.STREAM_ROUTE_PREFIX}remote-node",
                {
                    it[RedisMessageRouter.FIELD_FROM] == "7" &&
                            it[RedisMessageRouter.FIELD_TO] == "6" &&
                            it[RedisMessageRouter.FIELD_PAYLOAD_KIND] == RedisMessageRouter.PAYLOAD_KIND_IM_BINARY &&
                            Base64.decoder.decode(it[RedisMessageRouter.FIELD_BODY] as String) == payload.toByteArray()
                }
        )
        0 * clusterLocalDeliveryService._
    }

    def "should publish signal payload using text envelope when target user is remote"() {
        given:
        def signalPayload = '{"type":"offer","fromUser":"7"}'
        onlineService.getUserNodeId(6L) >> "remote-node"

        when:
        router.sendSignal(7L, 6L, signalPayload)

        then:
        1 * streamOperations.add(
                "${RedisMessageRouter.STREAM_ROUTE_PREFIX}remote-node",
                {
                    it[RedisMessageRouter.FIELD_PAYLOAD_KIND] == RedisMessageRouter.PAYLOAD_KIND_SIGNAL_TEXT &&
                            new String(Base64.decoder.decode(it[RedisMessageRouter.FIELD_BODY] as String), StandardCharsets.UTF_8) == signalPayload
                }
        )
        0 * clusterLocalDeliveryService._
    }

    def "should skip delivery when user is offline"() {
        given:
        onlineService.getUserNodeId(6L) >> null

        when:
        router.send(7L, 6L, sampleMessage("offline"))

        then:
        0 * streamOperations._
        0 * clusterLocalDeliveryService._
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
}
