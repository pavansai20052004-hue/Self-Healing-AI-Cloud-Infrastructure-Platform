package com.aegiscloud.incident.model;

import java.time.Instant;
import java.util.UUID;

public record StoredIncidentReport(
        UUID reportId,
        String serviceName,
        String predictionType,
        String severity,
        Instant createdAt,
        String markdown,
        boolean persisted
) {
    public StoredIncidentReport {
        reportId = reportId == null ? UUID.randomUUID() : reportId;
        serviceName = requireText(serviceName, "serviceName");
        predictionType = requireText(predictionType, "predictionType");
        severity = requireText(severity, "severity");
        createdAt = createdAt == null ? Instant.now() : createdAt;
        markdown = markdown == null ? "" : markdown;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
