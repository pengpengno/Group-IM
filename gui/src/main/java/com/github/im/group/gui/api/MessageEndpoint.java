package com.github.im.group.gui.api;

import com.github.im.dto.PageResult;
import com.github.im.dto.session.MessageDTO;
import com.github.im.dto.session.MessagePayLoad;
import com.github.im.dto.session.MessagePullRequest;
import com.github.im.dto.session.MessageSearchRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

@HttpExchange(url = "/api/messages")
public interface MessageEndpoint {

    //  根据 MessageId 查询消息更多的详细信息
    @PostExchange("/{msgId}")
    Mono<MessageDTO<MessagePayLoad>> getMessageById(@PathVariable Long msgId);

    // 拉取历史消息
    @PostExchange("/pull")
    PageResult<MessageDTO<MessagePayLoad>> pullHistoryMessages(@RequestBody MessagePullRequest request);


    // 标记消息为已读
    @PostExchange("/mark-as-read")
    Mono<Void> markAsRead(@RequestParam Long msgId);

    // 搜索消息
    @PostExchange("/search")
    Mono<PageResult<MessageDTO<MessagePayLoad>>> searchMessages(
        @RequestBody MessageSearchRequest request,
        @RequestParam Pageable pageable
    );
}