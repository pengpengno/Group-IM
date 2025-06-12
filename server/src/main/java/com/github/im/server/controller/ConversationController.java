package com.github.im.server.controller;

import com.github.im.conversation.ConversationRes;
import com.github.im.conversation.GroupInfo;
import com.github.im.server.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@Validated
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    /**
     * 创建新群组
     * @param groupInfo 群组信息，包括群组名称、描述和成员列表
     * @return 创建后的群组
     */
    @PostMapping("/group")
    public ResponseEntity<ConversationRes> createGroup(@RequestBody GroupInfo groupInfo) {
        var group = conversationService.createGroup(groupInfo.getGroupName(), groupInfo.getDescription(), groupInfo.getMembers());
        return ResponseEntity.ok(group);
    }

    /**
     * 创建或获取私聊会话
     *
     * @param userId1 第一个用户ID group creator
     * @param userId1 第二个用户ID
     * @return 私聊会话的DTO
     */
    @PostMapping("/private-chat")
    public ResponseEntity<ConversationRes> createOrGetPrivateChat(@RequestParam Long userId1, @RequestParam Long friendId) {
        ConversationRes conversationRes = conversationService.createOrGetPrivateChat(userId1, friendId);
        return ResponseEntity.ok(conversationRes);
    }

    /**
     * 获取某个用户正在进行的群组
     * @param userId 用户ID
     * @return 用户正在进行的群组
     */
    @GetMapping("/{userId}/active")
    public ResponseEntity<List<ConversationRes>> getActiveConversationsByUserId(@PathVariable Long userId) {
        var activeConversations = conversationService.getActiveConversationsByUserId(userId);
        return ResponseEntity.ok(activeConversations);
    }
}