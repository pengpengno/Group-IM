package com.github.im.server.workbench.integration.notification;

import com.github.im.dto.workbench.notification.WorkbenchCardEnvelope;
import com.github.im.dto.workbench.notification.WorkbenchEventEnvelope;
import com.github.im.dto.workbench.notification.WorkbenchProtocol;
import com.github.im.dto.workbench.notification.WorkbenchTarget;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class WorkbenchEnvelopeValidator {
    private static final Pattern ACTION_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");

    private final WorkbenchDeepLinkFactory deepLinkFactory;

    public WorkbenchEnvelopeValidator(WorkbenchDeepLinkFactory deepLinkFactory) {
        this.deepLinkFactory = deepLinkFactory;
    }

    public void validate(WorkbenchCardEnvelope envelope) {
        if (envelope == null) {
            throw invalid("card envelope is required");
        }
        validateVersion(envelope.version());
        validateEventId(envelope.eventId());
        validateTarget(new WorkbenchTarget(envelope.companyId(), envelope.category(), envelope.resourceId()));
        validateAction(envelope.action());
        requireText(envelope.title(), "title", WorkbenchProtocol.MAX_TITLE_LENGTH);
        requireText(envelope.fallbackText(), "fallbackText", WorkbenchProtocol.MAX_FALLBACK_TEXT_LENGTH);
        optionalText(envelope.summary(), "summary", WorkbenchProtocol.MAX_SUMMARY_LENGTH);
        optionalText(envelope.status(), "status", WorkbenchProtocol.MAX_STATUS_LENGTH);
        validateOccurredAt(envelope.occurredAt());
        validateDeepLink(
                new WorkbenchTarget(envelope.companyId(), envelope.category(), envelope.resourceId()),
                envelope.deepLink()
        );
    }

    public void validate(WorkbenchEventEnvelope envelope) {
        if (envelope == null) {
            throw invalid("event envelope is required");
        }
        validateVersion(envelope.version());
        validateEventId(envelope.eventId());
        validateTarget(new WorkbenchTarget(envelope.companyId(), envelope.category(), envelope.resourceId()));
        validateAction(envelope.action());
        validateOccurredAt(envelope.occurredAt());
        validateDeepLink(
                new WorkbenchTarget(envelope.companyId(), envelope.category(), envelope.resourceId()),
                envelope.deepLink()
        );
    }

    public void validateTarget(WorkbenchTarget target) {
        if (target == null) {
            throw invalid("target is required");
        }
        if (target.companyId() == null || target.companyId() <= 0) {
            throw invalid("companyId must be positive");
        }
        if (target.category() == null) {
            throw invalid("category is required");
        }
        requireText(target.resourceId(), "resourceId", WorkbenchProtocol.MAX_RESOURCE_ID_LENGTH);
    }

    private void validateVersion(int version) {
        if (version != WorkbenchProtocol.VERSION_1) {
            throw invalid("unsupported Workbench protocol version: " + version);
        }
    }

    private void validateEventId(String eventId) {
        requireText(eventId, "eventId", 36);
        try {
            UUID.fromString(eventId);
        } catch (IllegalArgumentException exception) {
            throw invalid("eventId must be a UUID");
        }
    }

    private void validateAction(String action) {
        requireText(action, "action", WorkbenchProtocol.MAX_ACTION_LENGTH);
        if (!ACTION_PATTERN.matcher(action).matches()) {
            throw invalid("action must be a stable uppercase protocol value");
        }
    }

    private void validateOccurredAt(Instant occurredAt) {
        if (occurredAt == null) {
            throw invalid("occurredAt is required");
        }
    }

    private void validateDeepLink(WorkbenchTarget target, String deepLink) {
        requireText(deepLink, "deepLink", 512);
        URI uri;
        try {
            uri = URI.create(deepLink);
        } catch (IllegalArgumentException exception) {
            throw invalid("deepLink must be a valid URI");
        }
        if (!"group".equals(uri.getScheme()) || !"workbench".equals(uri.getHost())) {
            throw invalid("deepLink must use the canonical group://workbench route");
        }
        String expected = deepLinkFactory.create(target);
        if (!expected.equals(deepLink)) {
            throw invalid("deepLink must match the canonical Workbench target");
        }
    }

    private void requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw invalid(field + " is required");
        }
        if (value.length() > maxLength) {
            throw invalid(field + " exceeds max length " + maxLength);
        }
    }

    private void optionalText(String value, String field, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw invalid(field + " exceeds max length " + maxLength);
        }
    }

    private IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException("Invalid Workbench protocol: " + message);
    }
}
