package com.github.im.group.gui.api;

import com.github.im.dto.session.MessageDTO;
import com.github.im.dto.session.MessageSearchRequest;
import com.github.im.dto.user.UserInfo;
import com.github.im.dto.user.UserRegisterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.GetExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@HttpExchange(url = "/api/messages")
public interface MessageEndpoint {

    // 发送消息
    @PostExchange("/send")
    Mono<List<MessageDTO>> sendMessage(@RequestBody List<MessageDTO> messages);

    // 拉取历史消息
    @GetExchange("/pull")
    Mono<Page<MessageDTO>> pullHistoryMessages(
        @RequestParam Long sessionId,
        @RequestParam(required = false) Long fromMessageId,
        @RequestParam(required = false) Long toMessageId,
        @RequestParam(required = false) LocalDateTime startTime,
        @RequestParam(required = false) LocalDateTime endTime,
        @RequestParam Pageable pageable
    );

    // 标记消息为已读
    @PostExchange("/mark-as-read")
    Mono<Void> markAsRead(@RequestParam Long msgId);

    // 搜索消息
    @PostExchange("/search")
    Mono<Page<MessageDTO>> searchMessages(
        @RequestBody MessageSearchRequest request,
        @RequestParam Pageable pageable
    );
}