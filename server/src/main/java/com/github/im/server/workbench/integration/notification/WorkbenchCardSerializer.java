package com.github.im.server.workbench.integration.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.im.dto.workbench.notification.WorkbenchCardEnvelope;
import org.springframework.stereotype.Component;

@Component
public class WorkbenchCardSerializer {
    private final ObjectMapper objectMapper;
    private final WorkbenchEnvelopeValidator validator;

    public WorkbenchCardSerializer(ObjectMapper objectMapper, WorkbenchEnvelopeValidator validator) {
        this.objectMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.validator = validator;
    }

    public String serialize(WorkbenchCardEnvelope envelope) {
        validator.validate(envelope);
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize Workbench card", exception);
        }
    }

    public WorkbenchCardEnvelope deserialize(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Workbench card content is required");
        }
        try {
            WorkbenchCardEnvelope envelope = objectMapper.readValue(content, WorkbenchCardEnvelope.class);
            validator.validate(envelope);
            return envelope;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to deserialize Workbench card", exception);
        }
    }
}
