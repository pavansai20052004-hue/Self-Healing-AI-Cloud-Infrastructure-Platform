package com.aegiscloud.core.autopilot;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record CounterfactualRemediationPlan(
        String planId,
        Instant generatedAt,
        String serviceName,
        String incidentType,
        String executionMode,
        String recommendation,
        double overallConfidence,
        double expectedRecoveryProbability,
        double residualRiskScore,
        double maxBlastRadiusPercent,
        boolean requiresApproval,
        List<ActionImpactEstimate> impactEstimates,
        List<RemediationStep> steps,
        List<String> rollbackTriggers,
        List<String> operatorPrompts
) {
    public CounterfactualRemediationPlan {
        planId = requireText(planId, "planId");
        generatedAt = Objects.requireNonNullElse(generatedAt, Instant.now());
        serviceName = requireText(serviceName, "serviceName");
        incidentType = requireText(incidentType, "incidentType");
        executionMode = requireText(executionMode, "executionMode");
        recommendation = requireText(recommendation, "recommendation");
        overallConfidence = clamp01(overallConfidence);
        expectedRecoveryProbability = clamp01(expectedRecoveryProbability);
        residualRiskScore = clamp01(residualRiskScore);
        maxBlastRadiusPercent = clampPercent(maxBlastRadiusPercent);
        impactEstimates = impactEstimates == null ? List.of() : List.copyOf(impactEstimates);
        steps = steps == null ? List.of() : List.copyOf(steps);
        rollbackTriggers = normalizeTextList(rollbackTriggers);
        operatorPrompts = normalizeTextList(operatorPrompts);
    }

    public boolean hasRunnableStep() {
        return steps.stream().anyMatch(step -> step.phase() == AutopilotPhase.REMEDIATE);
    }

    private static List<String> normalizeTextList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, Math.round(value * 100.0) / 100.0));
    }

    private static double clampPercent(double value) {
        return Math.max(0.0, Math.min(100.0, Math.round(value * 100.0) / 100.0));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
