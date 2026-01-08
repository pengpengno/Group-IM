package com.github.im.server.controller;

import com.github.im.dto.message.MessageDTO;
import com.github.im.dto.message.MessagePayLoad;
import com.github.im.dto.message.MessagePullRequest;
import com.github.im.dto.message.MessageSearchRequest;
import com.github.im.server.model.User;
import com.github.im.server.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    // 根据信息Id 查询
    @GetMapping("/{msgId}")
    public ResponseEntity<MessageDTO<MessagePayLoad>> getMessageById(@PathVariable Long msgId) {
        MessageDTO<MessagePayLoad> message = messageService.getMessageById(msgId);
        return ResponseEntity.ok(message);
    }

    // 拉取历史消息
    @PostMapping("/pull")
    public ResponseEntity<PagedModel<MessageDTO<MessagePayLoad>>> pullHistoryMessages(@RequestBody MessagePullRequest request) {


        Page<MessageDTO<MessagePayLoad>> messages = messageService.pullHistoryMessages(request );

        return ResponseEntity.ok(new PagedModel<>(messages));
    }


    // 标记消息为已读
    @PostMapping("/mark-as-read")
    public ResponseEntity<Void> markAsRead(@RequestParam Long msgId ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        messageService.markAsRead(msgId,(User) principal);
        return ResponseEntity.ok().build();
    }

    // 搜索消息
    @PostMapping("/search")
    public ResponseEntity<Page<MessageDTO<MessagePayLoad>>> searchMessages(@RequestBody MessageSearchRequest request,
                                                                           @PageableDefault(size = 50) Pageable pageable) {
        Page<MessageDTO<MessagePayLoad>> messages = messageService.searchMessages(request, pageable);
        return ResponseEntity.ok(messages);
    }
}