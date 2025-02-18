package com.github.im.server.controller;

import com.github.im.dto.session.MessageDTO;
import com.github.im.enums.MessageType;
import com.github.im.server.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    // 发送消息
    @PostMapping("/send")
    public ResponseEntity<MessageDTO> sendMessage(@RequestParam Long sessionId,
                                                  @RequestParam String content,
                                                  @RequestParam Long fromAccountId,
                                                  @RequestParam Long toAccountId,
                                                  @RequestParam MessageType type) {
        MessageDTO messageDTO = messageService.sendMessage(sessionId, content, fromAccountId, toAccountId, type);
        return ResponseEntity.ok(messageDTO);
    }



    @PostMapping("/pull")
    public ResponseEntity<MessageDTO> listUnReadMessages(@RequestParam Long sessionId,
                                                  @RequestParam String content,
                                                  @RequestParam Long fromAccountId,
                                                  @RequestParam Long toAccountId,
                                                  @RequestParam MessageType type) {
        MessageDTO messageDTO = messageService.sendMessage(sessionId, content, fromAccountId, toAccountId, type);
        return ResponseEntity.ok(messageDTO);
    }





    // 获取会话中的所有消息
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<MessageDTO>> getMessages(@PathVariable Long sessionId) {
        List<MessageDTO> messages = messageService.getMessages(sessionId);
        return ResponseEntity.ok(messages);
    }


}