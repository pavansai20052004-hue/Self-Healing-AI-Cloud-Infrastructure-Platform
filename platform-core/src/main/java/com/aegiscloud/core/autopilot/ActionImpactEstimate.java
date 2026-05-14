package com.aegiscloud.core.autopilot;

import com.aegiscloud.core.domain.HealingActionType;
import com.aegiscloud.core.governance.GuardrailVerdict;

import java.util.Objects;

public record ActionImpactEstimate(
        HealingActionType actionType,
        GuardrailVerdict guardrailVerdict,
        double recoveryProbability,
        double userImpactReductionPercent,
        double blastRadiusPercent,
        double confidence,
        double utilityScore,
        String rationale
) {
    public ActionImpactEstimate {
        actionType = Objects.requireNonNull(actionType, "actionType is required");
        guardrailVerdict = guardrailVerdict == null ? GuardrailVerdict.BLOCKED : guardrailVerdict;
        recoveryProbability = clamp01(recoveryProbability);
        userImpactReductionPercent = clampPercent(userImpactReductionPercent);
        blastRadiusPercent = clampPercent(blastRadiusPercent);
        confidence = clamp01(confidence);
        utilityScore = round(utilityScore);
        rationale = requireText(rationale, "rationale");
    }

    public boolean canRunAutomatically() {
        return guardrailVerdict == GuardrailVerdict.APPROVED;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, round(value)));
    }

    private static double clampPercent(double value) {
        return Math.max(0.0, Math.min(100.0, round(value)));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
