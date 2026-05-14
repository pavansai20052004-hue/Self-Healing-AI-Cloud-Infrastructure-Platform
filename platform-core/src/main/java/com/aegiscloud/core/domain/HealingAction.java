package com.aegiscloud.core.domain;

import java.util.Objects;

public record HealingAction(
        HealingActionType type,
        String targetService,
        String reason,
        String commandPreview,
        String riskLevel,
        int estimatedRecoverySeconds
) {
    public HealingAction {
        type = Objects.requireNonNull(type, "type is required");
        targetService = requireText(targetService, "targetService");
        reason = requireText(reason, "reason");
        commandPreview = requireText(commandPreview, "commandPreview");
        riskLevel = riskLevel == null || riskLevel.isBlank() ? "medium" : riskLevel.trim().toLowerCase();
        estimatedRecoverySeconds = Math.max(5, estimatedRecoverySeconds);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}

