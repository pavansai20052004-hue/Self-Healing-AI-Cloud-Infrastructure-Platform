package com.aegiscloud.core.incident;

import com.aegiscloud.core.domain.HealingDecision;
import com.aegiscloud.core.domain.IncidentSeverity;
import com.aegiscloud.core.domain.Prediction;
import com.aegiscloud.core.slo.SloBurnRate;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public final class IncidentLifecycleManager {
    public IncidentRecord open(
            String tenantId,
            String serviceName,
            List<Prediction> predictions,
            HealingDecision decision,
            SloBurnRate burnRate
    ) {
        IncidentSeverity severity = predictions == null || predictions.isEmpty()
                ? IncidentSeverity.INFO
                : predictions.stream()
                .map(Prediction::severity)
                .max(Comparator.comparingInt(IncidentLifecycleManager::severityRank))
                .orElse(IncidentSeverity.INFO);

        String summary = "Opened incident for " + serviceName + " with "
                + (predictions == null ? 0 : predictions.size())
                + " prediction(s) and SLO status " + (burnRate == null ? "unknown" : burnRate.status()) + ".";

        return new IncidentRecord(
                null,
                tenantId,
                serviceName,
                IncidentStatus.DETECTED,
                severity,
                predictions,
                decision,
                burnRate,
                Instant.now(),
                Instant.now(),
                summary
        );
    }

    public IncidentRecord afterGuardrails(IncidentRecord incident) {
        if (incident.decision() == null || incident.decision().actions().isEmpty()) {
            return incident.transitionTo(IncidentStatus.ESCALATED, "No automated mitigation was available.");
        }
        if (incident.decision().requiresApproval()) {
            return incident.transitionTo(IncidentStatus.TRIAGED, "Guardrails require operator approval before execution.");
        }
        if (incident.decision().hasExecutableAction()) {
            return incident.transitionTo(IncidentStatus.MITIGATING, "Approved remediation action is ready for execution.");
        }
        return incident.transitionTo(IncidentStatus.ESCALATED, "All remediation actions were blocked by guardrails.");
    }

    public IncidentRecord verify(IncidentRecord incident, boolean recovered) {
        if (recovered) {
            return incident.transitionTo(IncidentStatus.RESOLVED, "Post-healing verification passed and SLOs recovered.");
        }
        return incident.transitionTo(IncidentStatus.ESCALATED, "Post-healing verification failed; human review is required.");
    }

    private static int severityRank(IncidentSeverity severity) {
        return switch (severity) {
            case INFO -> 1;
            case WARNING -> 2;
            case CRITICAL -> 3;
        };
    }
}

