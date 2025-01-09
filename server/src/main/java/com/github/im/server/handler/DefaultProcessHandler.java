package com.github.im.server.handler;

import com.github.im.common.connect.connection.ConnectionConstants;
import com.github.im.common.connect.connection.server.ProtoBufProcessHandler;
import com.github.im.common.connect.model.proto.BaseMessage;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Sinks;
import reactor.netty.Connection;

import java.util.Optional;

/**
 *  DefaultProtobuf Process  ProcessHandler
 */
@Component
@Slf4j
public class DefaultProcessHandler implements ProtoBufProcessHandler {


    @Override
    public void process(@NotNull Connection con, BaseMessage.BaseMessagePkg message) {

        Hooks.onOperatorDebug();
        log.info("DefaultProcessHandler");


    }
}
