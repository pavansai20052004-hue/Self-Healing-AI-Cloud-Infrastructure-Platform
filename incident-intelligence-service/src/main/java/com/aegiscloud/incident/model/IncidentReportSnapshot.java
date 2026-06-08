package com.aegiscloud.incident.model;

import java.time.Instant;
import java.util.UUID;

public record IncidentReportSnapshot(
        UUID reportId,
        String serviceName,
        String predictionType,
        String severity,
        String predictionJson,
        String decisionJson,
        String recentLogsJson,
        String markdown,
        Instant createdAt
) {
    public IncidentReportSnapshot {
        reportId = reportId == null ? UUID.randomUUID() : reportId;
        serviceName = requireText(serviceName, "serviceName");
        predictionType = requireText(predictionType, "predictionType");
        severity = requireText(severity, "severity");
        predictionJson = requireText(predictionJson, "predictionJson");
        decisionJson = requireText(decisionJson, "decisionJson");
        recentLogsJson = recentLogsJson == null || recentLogsJson.isBlank() ? "[]" : recentLogsJson.trim();
        markdown = requireText(markdown, "markdown");
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
