package com.aegiscloud.core.autopilot;

import java.util.Objects;

public record RemediationStep(
        int stepNumber,
        AutopilotPhase phase,
        String title,
        String commandPreview,
        String successCriteria,
        String rollbackTrigger,
        int waitSeconds,
        double blastRadiusPercent
) {
    public RemediationStep {
        stepNumber = Math.max(1, stepNumber);
        phase = Objects.requireNonNull(phase, "phase is required");
        title = requireText(title, "title");
        commandPreview = requireText(commandPreview, "commandPreview");
        successCriteria = requireText(successCriteria, "successCriteria");
        rollbackTrigger = requireText(rollbackTrigger, "rollbackTrigger");
        waitSeconds = Math.max(0, waitSeconds);
        blastRadiusPercent = Math.max(0.0, Math.min(100.0, Math.round(blastRadiusPercent * 100.0) / 100.0));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
