package com.github.im.server.controller;

import com.github.im.dto.PageResult;
import com.github.im.dto.session.MessageDTO;
import com.github.im.dto.session.MessagePullRequest;
import com.github.im.dto.session.MessageSearchRequest;
import com.github.im.server.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

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
    @PostMapping("/pull")
    public ResponseEntity<PagedModel<MessageDTO>> pullHistoryMessages(@RequestBody MessagePullRequest request,
                                                                PagedResourcesAssembler<MessageDTO> assembler ) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Optional.ofNullable(request.getSort()).orElse("createTime")).descending()
        );

        Page<MessageDTO> messages = messageService.pullHistoryMessages(
                request.getConversationId(),
                request.getStartTime(),
                request.getEndTime(),
                pageable
        );
//        return ResponseEntity.ok(messages);
        return ResponseEntity.ok(new PagedModel<>(messages));
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