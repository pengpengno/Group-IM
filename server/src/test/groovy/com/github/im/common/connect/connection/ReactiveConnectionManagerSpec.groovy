package com.github.im.common.connect.connection

import com.github.im.common.connect.connection.server.BindAttr
import com.github.im.common.connect.enums.PlatformType
import com.github.im.common.connect.model.proto.BaseMessage
import spock.lang.Specification

class ReactiveConnectionManagerSpec extends Specification {

    def cleanup() {
        BindAttr.getAllPlatformBindAttr("6").each {
            ReactiveConnectionManager.unSubscribe(it)
        }
    }

    def "registerSinkFlow should reuse active sink and replace terminated one"() {
        given:
        def attr = BindAttr.getBindAttr("6", PlatformType.DESKTOP)

        when:
        def first = ReactiveConnectionManager.registerSinkFlow(attr)
        def second = ReactiveConnectionManager.registerSinkFlow(attr)
        first.tryEmitComplete()
        def third = ReactiveConnectionManager.registerSinkFlow(attr)

        then:
        first.is(second)
        !first.is(third)
    }

    def "addBaseMessage should deliver to active desktop sink for all-platform push attr"() {
        given:
        def desktopAttr = BindAttr.getBindAttr("6", PlatformType.DESKTOP)
        def sink = ReactiveConnectionManager.registerSinkFlow(desktopAttr)
        def received = []
        def disposable = sink.asFlux().subscribe { received << it }
        def message = sampleMessage("push")

        when:
        ReactiveConnectionManager.addBaseMessage(BindAttr.getBindAttrForPush("6"), message)

        then:
        received*.toByteArray() == [message.toByteArray()]

        cleanup:
        disposable.dispose()
        ReactiveConnectionManager.unSubscribe(desktopAttr)
    }

    def "addBaseMessage should ignore cancelled sink instead of throwing"() {
        given:
        def desktopAttr = BindAttr.getBindAttr("6", PlatformType.DESKTOP)
        def sink = ReactiveConnectionManager.registerSinkFlow(desktopAttr)
        def disposable = sink.asFlux().subscribe { }
        disposable.dispose()
        sink.tryEmitComplete()

        when:
        ReactiveConnectionManager.addBaseMessage(BindAttr.getBindAttrForPush("6"), sampleMessage("cancelled"))

        then:
        noExceptionThrown()
        !ReactiveConnectionManager.isSubscribe(desktopAttr)
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
