package com.github.im.server.controller;

import com.github.im.dto.message.MessageDTO;
import com.github.im.server.ai.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai-bot")
@CrossOrigin
public class AiBotController {

    private final MessageRouter messageRouter;

    @Autowired
    public AiBotController(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }

    @PostMapping("/message")
    public ResponseEntity<BotReply> handleMessage(@RequestBody MessageDTO<?> message) {
        BotReply reply = messageRouter.route(message);
        return ResponseEntity.ok(reply);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Bot service is running");
    }
}