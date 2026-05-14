package com.aegiscloud.core.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record Prediction(
        String predictionId,
        String serviceName,
        IncidentType type,
        IncidentSeverity severity,
        double confidence,
        String summary,
        List<String> evidence,
        HealingActionType recommendedAction,
        Instant createdAt
) {
    public Prediction {
        predictionId = requireText(predictionId, "predictionId");
        serviceName = requireText(serviceName, "serviceName");
        type = Objects.requireNonNull(type, "type is required");
        severity = Objects.requireNonNull(severity, "severity is required");
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        summary = requireText(summary, "summary");
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        recommendedAction = Objects.requireNonNull(recommendedAction, "recommendedAction is required");
        createdAt = Objects.requireNonNullElse(createdAt, Instant.now());
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}

