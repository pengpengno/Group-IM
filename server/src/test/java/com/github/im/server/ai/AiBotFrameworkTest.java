package com.github.im.server.ai;

import com.github.im.dto.message.MessageDTO;
import com.github.im.enums.MessageStatus;
import com.github.im.enums.MessageType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.testng.annotations.Test;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AiBotFrameworkTest {

    @Autowired
    @Qualifier("aiMessageRouter")
    private MessageRouter messageRouter;

    @org.testng.annotations.Test
    public void testAiSummaryBot() {
        // 创建测试消息
        MessageDTO<?> message = new MessageDTO<>();
        message.setContent("/summary 这是一段需要总结的内容");
        message.setConversationId(1L);
        message.setFromAccountId(100L);
        message.setType(MessageType.TEXT);
        message.setStatus(MessageStatus.SENT);

        // 路由消息
        BotReply reply = messageRouter.route(message);

        // 验证回复
        assertNotNull(reply);
        assertNotNull(reply.getContent());
        System.out.println("Summary Bot Response: " + reply.getContent());
    }

    @Test
    public void testAiToolBot() {
        // 创建测试消息
        MessageDTO<?> message = new MessageDTO<>();
        message.setContent("我想知道某个用户的信息");
        message.setConversationId(1L);
        message.setFromAccountId(100L);
        message.setType(MessageType.TEXT);
        message.setStatus(MessageStatus.SENT);

        // 路由消息
        BotReply reply = messageRouter.route(message);

        // 验证回复
        assertNotNull(reply);
        assertNotNull(reply.getContent());
        System.out.println("Tool Bot Response: " + reply.getContent());
    }

    @Test
    public void testAiFunctionCallingBot() {
        // 创建测试消息
        MessageDTO<?> message = new MessageDTO<>();
        message.setContent("/ask getUserInfo 12345");
        message.setConversationId(1L);
        message.setFromAccountId(100L);
        message.setType(MessageType.TEXT);
        message.setStatus(MessageStatus.SENT);

        // 路由消息
        BotReply reply = messageRouter.route(message);

        // 验证回复
        assertNotNull(reply);
        assertNotNull(reply.getContent());
        assertTrue(reply.getContent().contains("User ID: 12345"));
        System.out.println("Function Calling Bot Response: " + reply.getContent());
    }
}