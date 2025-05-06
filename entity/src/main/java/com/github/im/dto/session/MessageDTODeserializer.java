package com.github.im.dto.session;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.im.enums.MessageType;

import java.io.IOException;

public class MessageDTODeserializer extends JsonDeserializer<MessageDTO<?>> {

    @Override
    public MessageDTO<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = p.getCodec();
        ObjectNode root = codec.readTree(p);

        MessageType type = MessageType.fromCode(root.get("type").asInt());

        Class<? extends MessagePayLoad> payloadClass = getPayloadClassByType(type);

        ObjectNode payloadNode = (ObjectNode) root.get("payload");
        MessagePayLoad payload = null;
        if (payloadNode != null && payloadClass != null) {
            payload = codec.treeToValue(payloadNode, payloadClass);
        }

        // 反序列化整个 DTO，payload 先设为 null
        MessageDTO dto = codec.treeToValue(root, MessageDTO.class);
        dto.setPayload(payload);
        return dto;
    }

    private Class<? extends MessagePayLoad> getPayloadClassByType(MessageType type) {
        return switch (type) {
            case TEXT -> DefaultMessagePayLoad.class;
            case FILE -> FileMeta.class;
            default -> null;
        };
    }
}
