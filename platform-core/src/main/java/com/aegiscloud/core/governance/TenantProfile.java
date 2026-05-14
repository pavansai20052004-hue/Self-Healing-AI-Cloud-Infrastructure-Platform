package com.aegiscloud.core.governance;

import com.aegiscloud.core.domain.HealingActionType;

import java.util.Set;

public record TenantProfile(
        String tenantId,
        String displayName,
        String environment,
        AutonomyMode autonomyMode,
        boolean highRiskApprovalRequired,
        int maxReplicas,
        int maxActionsPerIncident,
        int actionCooldownSeconds,
        double maxAllowedBurnRate,
        Set<HealingActionType> allowedActions
) {
    public TenantProfile {
        tenantId = requireText(tenantId, "tenantId");
        displayName = requireText(displayName, "displayName");
        environment = requireText(environment, "environment");
        autonomyMode = autonomyMode == null ? AutonomyMode.DRY_RUN : autonomyMode;
        maxReplicas = Math.max(1, maxReplicas);
        maxActionsPerIncident = Math.max(1, maxActionsPerIncident);
        actionCooldownSeconds = Math.max(0, actionCooldownSeconds);
        maxAllowedBurnRate = Math.max(0.5, maxAllowedBurnRate);
        allowedActions = allowedActions == null || allowedActions.isEmpty()
                ? Set.of(HealingActionType.OPEN_INCIDENT)
                : Set.copyOf(allowedActions);
    }

    public static TenantProfile productionDefault(String tenantId, String displayName) {
        return new TenantProfile(
                tenantId,
                displayName,
                "production",
                AutonomyMode.SUPERVISED,
                true,
                8,
                3,
                180,
                14.4,
                Set.of(
                        HealingActionType.SCALE_OUT,
                        HealingActionType.RESTART_SERVICE,
                        HealingActionType.ROLLBACK_DEPLOYMENT,
                        HealingActionType.CLEAR_QUEUE,
                        HealingActionType.THROTTLE_TRAFFIC,
                        HealingActionType.PRUNE_LOGS,
                        HealingActionType.OPEN_INCIDENT
                )
        );
    }

    public boolean allows(HealingActionType actionType) {
        return allowedActions.contains(actionType);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}

