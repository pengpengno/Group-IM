package com.github.im.server.controller;

import com.github.im.dto.session.MessageDTO;
import com.github.im.dto.session.MessageSearchRequest;
import com.github.im.enums.MessageType;
import com.github.im.server.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    // 发送消息
    @PostMapping("/send")
    public ResponseEntity<List<MessageDTO>> sendMessage(@RequestBody List<MessageDTO> messages) {
        List<MessageDTO> sentMessages = messageService.sendMessages(messages);
        return ResponseEntity.ok(sentMessages);
    }

    // 拉取历史消息
    @GetMapping("/pull")
    public ResponseEntity<Page<MessageDTO>> pullHistoryMessages(@RequestParam Long sessionId,
                                                                @RequestParam(required = false) Long fromMessageId,
                                                                @RequestParam(required = false) Long toMessageId,
                                                                @RequestParam(required = false) LocalDateTime startTime,
                                                                @RequestParam(required = false) LocalDateTime endTime,
                                                                @PageableDefault(size = 50) Pageable pageable) {
        Page<MessageDTO> messages = messageService.pullHistoryMessages(sessionId, fromMessageId, toMessageId, startTime, endTime, pageable);
        return ResponseEntity.ok(messages);
    }

    // 标记消息为已读
    @PostMapping("/mark-as-read")
    public ResponseEntity<Void> markAsRead(@RequestParam Long msgId) {
        messageService.markAsRead(msgId);
        return ResponseEntity.ok().build();
    }

    // 搜索消息
    @PostMapping("/search")
    public ResponseEntity<Page<MessageDTO>> searchMessages(@RequestBody MessageSearchRequest request,
                                                           @PageableDefault(size = 50) Pageable pageable) {
        Page<MessageDTO> messages = messageService.searchMessages(request, pageable);
        return ResponseEntity.ok(messages);
    }
}