package com.github.im.server.workbench.integration.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.im.dto.workbench.notification.WorkbenchCardEnvelope;
import com.github.im.dto.workbench.notification.WorkbenchCategory;
import com.github.im.dto.workbench.notification.WorkbenchEventEnvelope;
import com.github.im.dto.workbench.notification.WorkbenchProtocol;
import com.github.im.dto.workbench.notification.WorkbenchTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WorkbenchProtocolTest {
    private WorkbenchDeepLinkFactory deepLinkFactory;
    private WorkbenchEnvelopeValidator validator;
    private WorkbenchCardSerializer serializer;

    @BeforeEach
    void setUp() {
        deepLinkFactory = new WorkbenchDeepLinkFactory();
        validator = new WorkbenchEnvelopeValidator(deepLinkFactory);
        serializer = new WorkbenchCardSerializer(new ObjectMapper().findAndRegisterModules(), validator);
    }

    @Test
    void createsCanonicalTenantAwareDeepLink() {
        String deepLink = deepLinkFactory.create(new WorkbenchTarget(42L, WorkbenchCategory.TASK, "901"));
        assertEquals("group://workbench/task/901?companyId=42", deepLink);
    }

    @Test
    void cardRoundTripsAndIgnoresUnknownFields() {
        WorkbenchCardEnvelope card = validCard();
        String json = serializer.serialize(card);
        String withFutureField = json.substring(0, json.length() - 1) + ",\"futureField\":\"ignored\"}";

        WorkbenchCardEnvelope decoded = serializer.deserialize(withFutureField);
        assertEquals(card, decoded);
    }

    @Test
    void rejectsNonCanonicalDeepLink() {
        WorkbenchCardEnvelope card = validCard();
        WorkbenchCardEnvelope forged = new WorkbenchCardEnvelope(
                card.version(), card.eventId(), card.category(), card.action(), card.resourceId(), card.companyId(),
                card.title(), card.summary(), card.fallbackText(), card.status(), card.occurredAt(),
                "group://workbench/task/901?companyId=99"
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> validator.validate(forged));
        assertTrue(exception.getMessage().contains("canonical"));
    }

    @Test
    void rejectsInvalidEventIdAndUnsupportedVersion() {
        WorkbenchCardEnvelope card = validCard();
        WorkbenchCardEnvelope invalidEvent = new WorkbenchCardEnvelope(
                card.version(), "not-a-uuid", card.category(), card.action(), card.resourceId(), card.companyId(),
                card.title(), card.summary(), card.fallbackText(), card.status(), card.occurredAt(), card.deepLink()
        );
        assertThrows(IllegalArgumentException.class, () -> validator.validate(invalidEvent));

        WorkbenchCardEnvelope futureVersion = new WorkbenchCardEnvelope(
                WorkbenchProtocol.VERSION_1 + 1, card.eventId(), card.category(), card.action(), card.resourceId(), card.companyId(),
                card.title(), card.summary(), card.fallbackText(), card.status(), card.occurredAt(), card.deepLink()
        );
        assertThrows(IllegalArgumentException.class, () -> validator.validate(futureVersion));
    }

    @Test
    void validatesRealtimeEnvelope() {
        WorkbenchCardEnvelope card = validCard();
        WorkbenchEventEnvelope event = new WorkbenchEventEnvelope(
                card.version(), card.eventId(), card.category(), card.action(), card.resourceId(), card.companyId(),
                card.occurredAt(), card.deepLink()
        );
        assertDoesNotThrow(() -> validator.validate(event));
    }

    @Test
    void policyKeyRequiresCategoryAndAction() {
        assertDoesNotThrow(() -> new WorkbenchNotificationPolicyKey(WorkbenchCategory.APPROVAL, "PENDING"));
        assertThrows(IllegalArgumentException.class, () -> new WorkbenchNotificationPolicyKey(null, "PENDING"));
        assertThrows(IllegalArgumentException.class, () -> new WorkbenchNotificationPolicyKey(WorkbenchCategory.APPROVAL, " "));
    }

    private WorkbenchCardEnvelope validCard() {
        WorkbenchTarget target = new WorkbenchTarget(42L, WorkbenchCategory.TASK, "901");
        return new WorkbenchCardEnvelope(
                WorkbenchProtocol.VERSION_1,
                UUID.randomUUID().toString(),
                target.category(),
                "ASSIGNED",
                target.resourceId(),
                target.companyId(),
                "任务已指派",
                "完成工作台接口",
                "[任务] 完成工作台接口",
                "TODO",
                Instant.parse("2026-08-19T08:30:00Z"),
                deepLinkFactory.create(target)
        );
    }
}
