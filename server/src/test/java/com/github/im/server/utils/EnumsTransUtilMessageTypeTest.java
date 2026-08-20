package com.github.im.server.utils;

import com.github.im.common.connect.model.proto.Chat;
import com.github.im.enums.MessageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnumsTransUtilMessageTypeTest {

    @Test
    void preservesExistingWireNumbersAndAppendsWorkbench() {
        assertEquals(0, Chat.MessageType.TEXT.getNumber());
        assertEquals(1, Chat.MessageType.FILE.getNumber());
        assertEquals(3, Chat.MessageType.VIDEO.getNumber());
        assertEquals(4, Chat.MessageType.VOICE.getNumber());
        assertEquals(6, Chat.MessageType.IMAGE.getNumber());
        assertEquals(7, Chat.MessageType.MEETING.getNumber());
        assertEquals(8, Chat.MessageType.BOT_CARD.getNumber());
        assertEquals(9, Chat.MessageType.WORKBENCH.getNumber());
    }

    @Test
    void mapsMeetingBotCardAndWorkbenchInBothDirections() {
        assertEquals(Chat.MessageType.MEETING, EnumsTransUtil.convertMessageType(MessageType.MEETING));
        assertEquals(Chat.MessageType.BOT_CARD, EnumsTransUtil.convertMessageType(MessageType.BOT_CARD));
        assertEquals(Chat.MessageType.WORKBENCH, EnumsTransUtil.convertMessageType(MessageType.WORKBENCH));

        assertEquals(MessageType.MEETING, EnumsTransUtil.convertMessageType(Chat.MessageType.MEETING));
        assertEquals(MessageType.BOT_CARD, EnumsTransUtil.convertMessageType(Chat.MessageType.BOT_CARD));
        assertEquals(MessageType.WORKBENCH, EnumsTransUtil.convertMessageType(Chat.MessageType.WORKBENCH));
    }

    @Test
    void keepsMediaOnExistingTextFallbackUntilItGetsAWireValue() {
        assertEquals(Chat.MessageType.TEXT, EnumsTransUtil.convertMessageType(MessageType.MEDIA));
    }
}
