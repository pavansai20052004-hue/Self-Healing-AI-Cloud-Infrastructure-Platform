package com.aegiscloud.core.audit;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record AuditEvent(
        String eventId,
        String tenantId,
        String actor,
        String eventType,
        String target,
        String reason,
        Map<String, String> attributes,
        Instant occurredAt
) {
    public AuditEvent {
        eventId = eventId == null || eventId.isBlank() ? "audit-" + UUID.randomUUID() : eventId.trim();
        tenantId = requireText(tenantId, "tenantId");
        actor = requireText(actor, "actor");
        eventType = requireText(eventType, "eventType");
        target = requireText(target, "target");
        reason = reason == null || reason.isBlank() ? "No reason supplied." : reason.trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        occurredAt = Objects.requireNonNullElse(occurredAt, Instant.now());
    }

    public static AuditEvent system(String tenantId, String eventType, String target, String reason) {
        return new AuditEvent(null, tenantId, "aegiscloud-system", eventType, target, reason, Map.of(), Instant.now());
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}

