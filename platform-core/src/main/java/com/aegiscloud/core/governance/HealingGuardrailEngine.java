package com.aegiscloud.core.governance;

import com.aegiscloud.core.domain.HealingAction;
import com.aegiscloud.core.domain.HealingActionType;
import com.aegiscloud.core.domain.HealingDecision;
import com.aegiscloud.core.domain.ServiceState;
import com.aegiscloud.core.slo.SloBurnRate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class HealingGuardrailEngine {
    public HealingDecision apply(
            HealingDecision decision,
            ServiceState state,
            TenantProfile profile,
            SloBurnRate burnRate
    ) {
        TenantProfile safeProfile = profile == null
                ? TenantProfile.productionDefault("default", "Default Tenant")
                : profile;
        List<GuardrailAssessment> assessments = new ArrayList<>();

        for (int index = 0; index < decision.actions().size(); index++) {
            HealingAction action = decision.actions().get(index);
            assessments.add(assess(action, index, decision, state, safeProfile, burnRate));
        }

        boolean forceDryRun = safeProfile.autonomyMode() == AutonomyMode.OBSERVE_ONLY
                || safeProfile.autonomyMode() == AutonomyMode.DRY_RUN
                || decision.dryRun();
        String executionMode = executionMode(safeProfile, forceDryRun, assessments);
        String explanation = decision.policyExplanation()
                + " Guardrails evaluated " + assessments.size()
                + " action(s) for tenant " + safeProfile.tenantId()
                + " in " + safeProfile.autonomyMode() + " mode.";

        return new HealingDecision(
                decision.decisionId(),
                decision.createdAt(),
                decision.prediction(),
                decision.actions(),
                forceDryRun,
                explanation,
                assessments,
                executionMode,
                assessments.stream().anyMatch(item -> item.verdict() == GuardrailVerdict.REQUIRES_APPROVAL)
        );
    }

    private static GuardrailAssessment assess(
            HealingAction action,
            int actionIndex,
            HealingDecision decision,
            ServiceState state,
            TenantProfile profile,
            SloBurnRate burnRate
    ) {
        if (!profile.allows(action.type())) {
            return assessment(action, GuardrailVerdict.BLOCKED, "Tenant policy does not allow " + action.type() + ".");
        }

        if (actionIndex >= profile.maxActionsPerIncident()) {
            return assessment(action, GuardrailVerdict.BLOCKED, "Action budget exceeded for this incident.");
        }

        if (action.type() == HealingActionType.SCALE_OUT && state != null && state.replicas() + 1 > profile.maxReplicas()) {
            return assessment(action, GuardrailVerdict.BLOCKED, "Scale-out would exceed tenant replica limit of " + profile.maxReplicas() + ".");
        }

        if (profile.autonomyMode() == AutonomyMode.OBSERVE_ONLY) {
            return assessment(action, GuardrailVerdict.DRY_RUN_ONLY, "Tenant is configured for observe-only recommendations.");
        }

        if (profile.autonomyMode() == AutonomyMode.DRY_RUN || decision.dryRun()) {
            return assessment(action, GuardrailVerdict.DRY_RUN_ONLY, "Dry-run mode is active, so command execution is disabled.");
        }

        if (isHighRisk(action) && profile.highRiskApprovalRequired()) {
            return assessment(action, GuardrailVerdict.REQUIRES_APPROVAL, "High-risk action requires operator approval.");
        }

        if (burnRate != null && burnRate.burnRate() > profile.maxAllowedBurnRate() && action.type() == HealingActionType.OPEN_INCIDENT) {
            return assessment(action, GuardrailVerdict.APPROVED, "Opening an incident is approved during high burn-rate pressure.");
        }

        if (profile.autonomyMode() == AutonomyMode.SUPERVISED && isMediumOrHighRisk(action)) {
            return assessment(action, GuardrailVerdict.REQUIRES_APPROVAL, "Supervised mode requires approval for medium or high risk remediation.");
        }

        return assessment(action, GuardrailVerdict.APPROVED, "Action passed tenant policy, autonomy, and SLO guardrails.");
    }

    private static boolean isHighRisk(HealingAction action) {
        return "high".equalsIgnoreCase(action.riskLevel())
                || action.type() == HealingActionType.ROLLBACK_DEPLOYMENT
                || action.type() == HealingActionType.INCREASE_MEMORY_LIMIT;
    }

    private static boolean isMediumOrHighRisk(HealingAction action) {
        return isHighRisk(action) || "medium".equalsIgnoreCase(action.riskLevel());
    }

    private static GuardrailAssessment assessment(HealingAction action, GuardrailVerdict verdict, String reason) {
        return new GuardrailAssessment(action, verdict, reason, Instant.now());
    }

    private static String executionMode(
            TenantProfile profile,
            boolean forceDryRun,
            List<GuardrailAssessment> assessments
    ) {
        if (profile.autonomyMode() == AutonomyMode.OBSERVE_ONLY) {
            return "observe-only";
        }
        if (forceDryRun) {
            return "dry-run";
        }
        if (assessments.stream().anyMatch(item -> item.verdict() == GuardrailVerdict.REQUIRES_APPROVAL)) {
            return "waiting-for-approval";
        }
        if (assessments.stream().anyMatch(GuardrailAssessment::canExecute)) {
            return "execute-approved";
        }
        return "blocked";
    }
}

