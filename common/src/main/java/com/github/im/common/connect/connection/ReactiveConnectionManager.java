package com.github.im.common.connect.connection;

import com.github.im.common.connect.connection.server.BindAttr;
import com.github.im.common.connect.enums.PlatformType;
import com.github.im.common.connect.model.proto.BaseMessage;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Scannable;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitResult;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Slf4j
public class ReactiveConnectionManager {

    private static final ConcurrentMap<BindAttr<String>, Sinks.Many<BaseMessage.BaseMessagePkg>> BASE_MESSAGE_SINKS =
            new ConcurrentHashMap<>();

    public static final String ALL_PLATFORM_PUSH_TAG = "ALL";

    public static Sinks.Many<BaseMessage.BaseMessagePkg> registerSinkFlow(BindAttr<String> attr) {
        return BASE_MESSAGE_SINKS.compute(attr, (key, existingSink) -> {
            if (isSinkActive(existingSink)) {
                return existingSink;
            }
            return Sinks.many().multicast().onBackpressureBuffer();
        });
    }

    public static Sinks.Many<BaseMessage.BaseMessagePkg> getSinkFlow(BindAttr<String> attr) {
        return BASE_MESSAGE_SINKS.get(attr);
    }

    public static boolean isSubscribe(BindAttr<String> attr) {
        return !getAllSubscribeAttr(attr).isEmpty();
    }

    public static void addBaseMessage(BindAttr<String> attr, BaseMessage.BaseMessagePkg baseMessagePkg) {
        List<BindAttr<String>> subscribeAttrs = getAllSubscribeAttr(attr);
        if (subscribeAttrs.isEmpty()) {
            log.debug("No active sink found for ATTR: {}", attr);
            return;
        }

        subscribeAttrs.forEach(bindAttr -> {
            log.info("推送消息 {} 到 {} ", baseMessagePkg, bindAttr.getKey());
            emitToSink(bindAttr, baseMessagePkg);
        });
    }

    public static List<BindAttr<String>> getAllSubscribeAttr(BindAttr<String> attr) {
        if (attr == null || attr.getKey() == null) {
            return emptyList();
        }

        String key = attr.getKey();
        if (key.contains(ALL_PLATFORM_PUSH_TAG)) {
            log.debug("推送全平台的标识");
            int i = key.lastIndexOf("_");
            String account = key.substring(0, i);
            return Arrays.stream(PlatformType.values())
                    .map(platformType -> {
                        BindAttr<String> bindAttr = BindAttr.getBindAttr(account, platformType);
                        if (isSinkActive(BASE_MESSAGE_SINKS.get(bindAttr))) {
                            return bindAttr;
                        }
                        cleanupInactiveSink(bindAttr);
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        if (isSinkActive(BASE_MESSAGE_SINKS.get(attr))) {
            return List.of(attr);
        }
        cleanupInactiveSink(attr);
        return emptyList();
    }

    public static void unSubscribe(BindAttr<String> attr) {
        if (attr == null) {
            return;
        }

        try {
            Sinks.Many<BaseMessage.BaseMessagePkg> sink = BASE_MESSAGE_SINKS.remove(attr);
            if (sink != null) {
                sink.tryEmitComplete();
            }
        } catch (Exception e) {
            log.error("unSubscribe error", e);
        }
    }

    private static void emitToSink(BindAttr<String> attr, BaseMessage.BaseMessagePkg baseMessagePkg) {
        Sinks.Many<BaseMessage.BaseMessagePkg> sink = BASE_MESSAGE_SINKS.get(attr);
        if (!isSinkActive(sink)) {
            cleanupInactiveSink(attr);
            log.debug("Skip push because sink is inactive, ATTR: {}", attr.getKey());
            return;
        }

        EmitResult result = sink.tryEmitNext(baseMessagePkg);
        if (result.isFailure()) {
            log.warn("Failed to emit message to {}, result={}", attr.getKey(), result);
            if (result == EmitResult.FAIL_CANCELLED || result == EmitResult.FAIL_TERMINATED) {
                cleanupInactiveSink(attr);
            }
        }
    }

    private static boolean isSinkActive(Sinks.Many<BaseMessage.BaseMessagePkg> sink) {
        if (sink == null) {
            return false;
        }

        Boolean cancelled = Scannable.from(sink).scan(Scannable.Attr.CANCELLED);
        Boolean terminated = Scannable.from(sink).scan(Scannable.Attr.TERMINATED);
        return !Boolean.TRUE.equals(cancelled) && !Boolean.TRUE.equals(terminated);
    }

    private static void cleanupInactiveSink(BindAttr<String> attr) {
        if (attr == null) {
            return;
        }

        Sinks.Many<BaseMessage.BaseMessagePkg> sink = BASE_MESSAGE_SINKS.get(attr);
        if (!isSinkActive(sink)) {
            BASE_MESSAGE_SINKS.remove(attr, sink);
        }
    }
}
