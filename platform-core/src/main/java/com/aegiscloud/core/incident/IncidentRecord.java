package com.aegiscloud.core.incident;

import com.aegiscloud.core.domain.HealingDecision;
import com.aegiscloud.core.domain.IncidentSeverity;
import com.aegiscloud.core.domain.Prediction;
import com.aegiscloud.core.slo.SloBurnRate;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record IncidentRecord(
        String incidentId,
        String tenantId,
        String serviceName,
        IncidentStatus status,
        IncidentSeverity severity,
        List<Prediction> predictions,
        HealingDecision decision,
        SloBurnRate sloBurnRate,
        Instant openedAt,
        Instant updatedAt,
        String summary
) {
    public IncidentRecord {
        incidentId = incidentId == null || incidentId.isBlank() ? "inc-" + UUID.randomUUID() : incidentId.trim();
        tenantId = requireText(tenantId, "tenantId");
        serviceName = requireText(serviceName, "serviceName");
        status = status == null ? IncidentStatus.DETECTED : status;
        severity = severity == null ? IncidentSeverity.INFO : severity;
        predictions = predictions == null ? List.of() : List.copyOf(predictions);
        openedAt = Objects.requireNonNullElse(openedAt, Instant.now());
        updatedAt = Objects.requireNonNullElse(updatedAt, openedAt);
        summary = summary == null || summary.isBlank() ? "No summary generated." : summary.trim();
    }

    public IncidentRecord transitionTo(IncidentStatus nextStatus, String nextSummary) {
        return new IncidentRecord(
                incidentId,
                tenantId,
                serviceName,
                nextStatus,
                severity,
                predictions,
                decision,
                sloBurnRate,
                openedAt,
                Instant.now(),
                nextSummary
        );
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}

