package com.aegiscloud.core.autopilot;

import com.aegiscloud.core.domain.HealingAction;
import com.aegiscloud.core.domain.HealingActionType;
import com.aegiscloud.core.domain.HealingDecision;
import com.aegiscloud.core.domain.IncidentType;
import com.aegiscloud.core.domain.ServiceState;
import com.aegiscloud.core.governance.AutonomyMode;
import com.aegiscloud.core.governance.GuardrailAssessment;
import com.aegiscloud.core.governance.GuardrailVerdict;
import com.aegiscloud.core.governance.TenantProfile;
import com.aegiscloud.core.slo.SloBurnRate;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class CounterfactualRemediationPlanner {
    public CounterfactualRemediationPlan plan(
            HealingDecision decision,
            ServiceState state,
            TenantProfile profile,
            SloBurnRate burnRate
    ) {
        Objects.requireNonNull(decision, "decision is required");
        ServiceState safeState = state == null
                ? ServiceState.defaultFor(decision.prediction().serviceName())
                : state;
        TenantProfile safeProfile = profile == null
                ? TenantProfile.productionDefault("default", "Default Tenant")
                : profile;

        List<GuardrailAssessment> assessments = normalizeAssessments(decision);
        List<ActionImpactEstimate> estimates = assessments.stream()
                .map(assessment -> estimate(decision, safeState, safeProfile, burnRate, assessment))
                .sorted(Comparator.comparingDouble(ActionImpactEstimate::utilityScore).reversed())
                .toList();
        GuardrailAssessment leadAssessment = selectLeadAssessment(assessments);
        ActionImpactEstimate leadEstimate = leadAssessment == null
                ? null
                : estimates.stream()
                .filter(estimate -> estimate.actionType() == leadAssessment.action().type())
                .findFirst()
                .orElse(null);

        double expectedRecovery = leadEstimate == null ? 0.0 : leadEstimate.recoveryProbability();
        double maxBlastRadius = estimates.stream()
                .mapToDouble(ActionImpactEstimate::blastRadiusPercent)
                .max()
                .orElse(0.0);
        double residualRisk = residualRisk(decision, safeProfile, burnRate, leadEstimate);
        List<RemediationStep> steps = buildSteps(decision, safeState, safeProfile, burnRate, leadAssessment, leadEstimate);
        List<String> rollbackTriggers = rollbackTriggers(decision, safeState, burnRate, leadAssessment);

        return new CounterfactualRemediationPlan(
                "plan-" + UUID.randomUUID(),
                Instant.now(),
                decision.prediction().serviceName(),
                decision.prediction().type().name(),
                decision.executionMode(),
                recommendation(decision, safeProfile, burnRate, leadAssessment, leadEstimate),
                leadEstimate == null ? 0.0 : leadEstimate.confidence(),
                expectedRecovery,
                residualRisk,
                maxBlastRadius,
                decision.requiresApproval(),
                estimates,
                steps,
                rollbackTriggers,
                operatorPrompts(decision, safeProfile, burnRate, leadAssessment, leadEstimate)
        );
    }

    private static List<GuardrailAssessment> normalizeAssessments(HealingDecision decision) {
        if (!decision.guardrailAssessments().isEmpty()) {
            return decision.guardrailAssessments();
        }
        GuardrailVerdict inferredVerdict = decision.dryRun()
                ? GuardrailVerdict.DRY_RUN_ONLY
                : GuardrailVerdict.APPROVED;
        return decision.actions().stream()
                .map(action -> new GuardrailAssessment(
                        action,
                        inferredVerdict,
                        "No explicit guardrail assessment was attached; planner inferred " + inferredVerdict + " from decision mode.",
                        Instant.now()
                ))
                .toList();
    }

    private static GuardrailAssessment selectLeadAssessment(List<GuardrailAssessment> assessments) {
        return assessments.stream()
                .filter(assessment -> assessment.verdict() == GuardrailVerdict.APPROVED)
                .findFirst()
                .or(() -> assessments.stream()
                        .filter(assessment -> assessment.verdict() == GuardrailVerdict.REQUIRES_APPROVAL)
                        .findFirst())
                .or(() -> assessments.stream()
                        .filter(assessment -> assessment.verdict() == GuardrailVerdict.DRY_RUN_ONLY)
                        .findFirst())
                .orElseGet(() -> assessments.isEmpty() ? null : assessments.getFirst());
    }

    private static ActionImpactEstimate estimate(
            HealingDecision decision,
            ServiceState state,
            TenantProfile profile,
            SloBurnRate burnRate,
            GuardrailAssessment assessment
    ) {
        HealingAction action = assessment.action();
        double recoveryProbability = baseRecoveryProbability(action.type(), decision.prediction().type(), state)
                + ((decision.prediction().confidence() - 0.50) * 0.20)
                + (burnRate != null && burnRate.isUrgent() ? 0.06 : 0.0)
                - riskPenalty(action);
        recoveryProbability *= guardrailMultiplier(assessment.verdict());

        double userImpactReduction = baseImpactReduction(action.type(), decision.prediction().type())
                + (burnRate == null ? 0.0 : Math.min(12.0, burnRate.burnRate() / 2.0));
        userImpactReduction *= guardrailMultiplier(assessment.verdict());

        double blastRadius = blastRadius(action, state, profile);
        double confidence = 0.52
                + (decision.prediction().confidence() * 0.34)
                + verdictConfidenceBonus(assessment.verdict())
                - (blastRadius / 400.0)
                - riskPenalty(action);
        double utility = (clamp01(recoveryProbability) * userImpactReduction)
                - (blastRadius * 0.35)
                - (riskPenalty(action) * 20.0);

        String rationale = "Counterfactual estimate blends prediction confidence, guardrail verdict "
                + assessment.verdict()
                + ", SLO pressure, action risk, and blast radius.";
        return new ActionImpactEstimate(
                action.type(),
                assessment.verdict(),
                recoveryProbability,
                userImpactReduction,
                blastRadius,
                confidence,
                utility,
                rationale
        );
    }

    private static List<RemediationStep> buildSteps(
            HealingDecision decision,
            ServiceState state,
            TenantProfile profile,
            SloBurnRate burnRate,
            GuardrailAssessment leadAssessment,
            ActionImpactEstimate leadEstimate
    ) {
        String service = decision.prediction().serviceName();
        RemediationPlanBuilder builder = new RemediationPlanBuilder();
        builder.add(
                AutopilotPhase.PREFLIGHT,
                "Capture live blast-radius snapshot",
                "aegisctl preflight capture --service " + service + " --window 10m",
                "Current metrics, deployment revision, replica count, and SLO window are captured.",
                "Abort if topology, deployment revision, or active incident owner changes during planning.",
                10,
                0
        );

        if (leadAssessment == null) {
            builder.add(
                    AutopilotPhase.ESCALATE,
                    "Escalate with no automated action",
                    "aegisctl incident open --service " + service + " --reason no-automated-remediation",
                    "Owning team receives the incident with attached telemetry.",
                    "No automated rollback is available because no action was selected.",
                    0,
                    0
            );
            return builder.steps();
        }

        HealingAction action = leadAssessment.action();
        double blastRadius = leadEstimate == null ? blastRadius(action, state, profile) : leadEstimate.blastRadiusPercent();

        if (leadAssessment.verdict() == GuardrailVerdict.BLOCKED) {
            builder.add(
                    AutopilotPhase.ESCALATE,
                    "Escalate blocked remediation",
                    "aegisctl incident escalate --service " + service + " --blocked-action " + action.type(),
                    "Operator receives the blocked action, policy reason, and suggested manual checks.",
                    "No automated execution is permitted while guardrail verdict is BLOCKED.",
                    0,
                    0
            );
            return builder.steps();
        }

        if (leadAssessment.verdict() == GuardrailVerdict.REQUIRES_APPROVAL) {
            builder.add(
                    AutopilotPhase.APPROVAL_GATE,
                    "Request approval with counterfactual evidence",
                    "aegisctl approval request --service " + service + " --action " + action.type()
                            + " --burn-rate " + burnRateLabel(burnRate),
                    "On-call owner approves or rejects the action using blast-radius and rollback evidence.",
                    "Abort if approval is rejected, expires, or a newer deployment starts.",
                    profile.actionCooldownSeconds(),
                    0
            );
        }

        if (leadAssessment.verdict() == GuardrailVerdict.DRY_RUN_ONLY || decision.dryRun()) {
            builder.add(
                    AutopilotPhase.CANARY,
                    "Run dry-run canary",
                    "aegisctl canary dry-run --service " + service + " --action " + action.type()
                            + " --max-blast-radius " + Math.round(blastRadius) + "%",
                    "Planner confirms the command, affected objects, and rollback path without mutating the cluster.",
                    "Abort if dry-run diff includes objects outside the service ownership boundary.",
                    30,
                    0
            );
            builder.add(
                    AutopilotPhase.VERIFY,
                    "Compare counterfactual recovery window",
                    "aegisctl verify counterfactual --service " + service + " --window 2m",
                    "Expected recovery probability remains above the action threshold.",
                    "Escalate if projected burn rate remains above tenant policy.",
                    120,
                    0
            );
            return builder.steps();
        }

        builder.add(
                AutopilotPhase.CANARY,
                "Limit action to first recovery cell",
                "aegisctl canary apply --service " + service + " --action " + action.type()
                        + " --max-blast-radius " + Math.round(Math.min(blastRadius, 25.0)) + "%",
                "Canary cell shows improving error rate, latency, and saturation before global rollout.",
                "Rollback if canary error rate rises or p95 latency stays above the SLO objective.",
                Math.min(90, action.estimatedRecoverySeconds()),
                Math.min(blastRadius, 25.0)
        );
        builder.add(
                AutopilotPhase.REMEDIATE,
                "Execute selected remediation",
                action.commandPreview(),
                "Primary service metrics return toward SLO and no dependent service enters warning state.",
                "Rollback if burn rate worsens, restart count increases, or guardrail drift is detected.",
                action.estimatedRecoverySeconds(),
                blastRadius
        );
        builder.add(
                AutopilotPhase.VERIFY,
                "Verify SLO recovery",
                "aegisctl verify slo --service " + service + " --window 2m --target-burn-rate 1.0",
                "Burn rate is below 1.0x, latency/error metrics stabilize, and audit events are persisted.",
                "Escalate if SLO recovery does not begin within two verification windows.",
                120,
                0
        );
        builder.add(
                AutopilotPhase.ROLLBACK_READY,
                "Keep rollback command armed",
                rollbackCommand(action, state),
                "Rollback command is ready but not executed unless verification fails.",
                "Execute rollback when verification fails or user impact expands beyond planned blast radius.",
                0,
                0
        );
        return builder.steps();
    }

    private static List<String> rollbackTriggers(
            HealingDecision decision,
            ServiceState state,
            SloBurnRate burnRate,
            GuardrailAssessment leadAssessment
    ) {
        String service = decision.prediction().serviceName();
        return List.of(
                "Burn rate remains above " + burnRateLabel(burnRate) + " after two verification windows.",
                "p95 latency or error rate worsens for " + service + " during canary.",
                "Replica count, deployment revision, or queue depth moves outside the captured preflight envelope.",
                leadAssessment == null
                        ? "No selected action exists; keep incident in human-led response."
                        : "Guardrail verdict changes from " + leadAssessment.verdict() + " before execution.",
                "Previous healthy version " + state.previousVersion() + " becomes unavailable."
        );
    }

    private static List<String> operatorPrompts(
            HealingDecision decision,
            TenantProfile profile,
            SloBurnRate burnRate,
            GuardrailAssessment leadAssessment,
            ActionImpactEstimate leadEstimate
    ) {
        String action = leadAssessment == null ? "no automated action" : leadAssessment.action().type().name();
        String probability = leadEstimate == null
                ? "0%"
                : Math.round(leadEstimate.recoveryProbability() * 100.0) + "%";
        String approvalPrompt = decision.requiresApproval()
                ? "Approval required: review deployment age, customer impact, and rollback evidence before allowing " + action + "."
                : "No approval gate is required by the current tenant policy.";
        String autonomyPrompt = profile.autonomyMode() == AutonomyMode.AUTONOMOUS
                ? "Autonomous execution is enabled; verify policy ownership and audit sinks before live use."
                : "Autonomy mode " + profile.autonomyMode() + " keeps the operator in the loop.";
        String sloPrompt = burnRate != null && burnRate.isUrgent()
                ? "SLO is under urgent pressure; page the owning team while remediation runs."
                : "SLO pressure is not urgent; prefer the lowest-blast-radius action.";
        return List.of(
                "Lead action " + action + " has estimated recovery probability " + probability + ".",
                approvalPrompt,
                autonomyPrompt,
                sloPrompt
        );
    }

    private static String recommendation(
            HealingDecision decision,
            TenantProfile profile,
            SloBurnRate burnRate,
            GuardrailAssessment leadAssessment,
            ActionImpactEstimate leadEstimate
    ) {
        if (leadAssessment == null) {
            return "No automated remediation is available; open an incident and attach the telemetry packet.";
        }
        if (leadAssessment.verdict() == GuardrailVerdict.BLOCKED) {
            return "Do not execute " + leadAssessment.action().type() + "; tenant guardrails blocked the action.";
        }
        if (leadAssessment.verdict() == GuardrailVerdict.REQUIRES_APPROVAL) {
            return "Request operator approval for " + leadAssessment.action().type()
                    + " with " + probabilityLabel(leadEstimate) + " projected recovery probability.";
        }
        if (leadAssessment.verdict() == GuardrailVerdict.DRY_RUN_ONLY || decision.dryRun()) {
            return "Keep remediation in dry-run and use the counterfactual plan to brief the operator.";
        }
        if (burnRate != null && burnRate.burnRate() > profile.maxAllowedBurnRate()) {
            return "Execute " + leadAssessment.action().type()
                    + " using canary-first rollout because SLO burn rate exceeds tenant policy.";
        }
        return "Execute " + leadAssessment.action().type()
                + " with canary verification and rollback command armed.";
    }

    private static String probabilityLabel(ActionImpactEstimate estimate) {
        if (estimate == null) {
            return "unknown";
        }
        return Math.round(estimate.recoveryProbability() * 100.0) + "%";
    }

    private static double residualRisk(
            HealingDecision decision,
            TenantProfile profile,
            SloBurnRate burnRate,
            ActionImpactEstimate leadEstimate
    ) {
        double base = 1.0 - (leadEstimate == null ? 0.0 : leadEstimate.recoveryProbability());
        double burnPressure = burnRate == null ? 0.12 : Math.min(0.34, burnRate.burnRate() / 100.0);
        double approvalPressure = decision.requiresApproval() ? 0.12 : 0.0;
        double autonomyPressure = profile.autonomyMode() == AutonomyMode.OBSERVE_ONLY ? 0.16 : 0.0;
        return clamp01(base + burnPressure + approvalPressure + autonomyPressure);
    }

    private static double baseRecoveryProbability(HealingActionType actionType, IncidentType incidentType, ServiceState state) {
        return switch (actionType) {
            case SCALE_OUT -> incidentType == IncidentType.CPU_SATURATION || incidentType == IncidentType.LATENCY_SPIKE ? 0.78 : 0.58;
            case RESTART_SERVICE -> incidentType == IncidentType.MEMORY_LEAK ? 0.72 : 0.58;
            case ROLLBACK_DEPLOYMENT -> state.wasRecentlyDeployed() ? 0.82 : 0.62;
            case CLEAR_QUEUE -> incidentType == IncidentType.LATENCY_SPIKE ? 0.70 : 0.52;
            case INCREASE_MEMORY_LIMIT -> incidentType == IncidentType.MEMORY_LEAK ? 0.66 : 0.44;
            case THROTTLE_TRAFFIC -> 0.50;
            case PRUNE_LOGS -> incidentType == IncidentType.DISK_PRESSURE ? 0.76 : 0.46;
            case OPEN_INCIDENT -> 0.32;
        };
    }

    private static double baseImpactReduction(HealingActionType actionType, IncidentType incidentType) {
        return switch (actionType) {
            case SCALE_OUT -> incidentType == IncidentType.CPU_SATURATION ? 58.0 : 42.0;
            case RESTART_SERVICE -> 48.0;
            case ROLLBACK_DEPLOYMENT -> 72.0;
            case CLEAR_QUEUE -> 52.0;
            case INCREASE_MEMORY_LIMIT -> 46.0;
            case THROTTLE_TRAFFIC -> 34.0;
            case PRUNE_LOGS -> 54.0;
            case OPEN_INCIDENT -> 12.0;
        };
    }

    private static double blastRadius(HealingAction action, ServiceState state, TenantProfile profile) {
        double base = switch (action.type()) {
            case SCALE_OUT -> Math.max(8.0, 100.0 / Math.max(1, state.replicas() + 1));
            case RESTART_SERVICE -> state.replicas() <= 1 ? 100.0 : Math.max(18.0, 100.0 / state.replicas());
            case ROLLBACK_DEPLOYMENT -> state.wasRecentlyDeployed() ? 62.0 : 78.0;
            case CLEAR_QUEUE -> state.queueDepth() > 2_000 ? 34.0 : 22.0;
            case INCREASE_MEMORY_LIMIT -> 66.0;
            case THROTTLE_TRAFFIC -> 38.0;
            case PRUNE_LOGS -> 28.0;
            case OPEN_INCIDENT -> 0.0;
        };
        if ("production".equalsIgnoreCase(profile.environment())) {
            base += 4.0;
        }
        return clampPercent(base);
    }

    private static double guardrailMultiplier(GuardrailVerdict verdict) {
        return switch (verdict) {
            case APPROVED -> 1.0;
            case DRY_RUN_ONLY -> 0.72;
            case REQUIRES_APPROVAL -> 0.82;
            case BLOCKED -> 0.18;
        };
    }

    private static double verdictConfidenceBonus(GuardrailVerdict verdict) {
        return switch (verdict) {
            case APPROVED -> 0.16;
            case REQUIRES_APPROVAL -> 0.08;
            case DRY_RUN_ONLY -> 0.03;
            case BLOCKED -> -0.16;
        };
    }

    private static double riskPenalty(HealingAction action) {
        return switch (action.riskLevel().toLowerCase()) {
            case "low" -> 0.02;
            case "high" -> 0.18;
            default -> 0.09;
        };
    }

    private static String rollbackCommand(HealingAction action, ServiceState state) {
        String service = action.targetService();
        return switch (action.type()) {
            case SCALE_OUT -> "kubectl scale deployment/" + service + " --replicas=" + state.replicas();
            case RESTART_SERVICE -> "kubectl rollout undo deployment/" + service + " --to-revision=" + state.previousVersion();
            case ROLLBACK_DEPLOYMENT -> "kubectl rollout history deployment/" + service + " && aegisctl deploy restore --service "
                    + service + " --version " + state.currentVersion();
            case CLEAR_QUEUE -> "aegisctl queue replay --service " + service + " --from-snapshot latest";
            case INCREASE_MEMORY_LIMIT -> "aegisctl resources revert --service " + service + " --field memory-limit";
            case THROTTLE_TRAFFIC -> "kubectl annotate service/" + service + " aegiscloud.io/throttle-";
            case PRUNE_LOGS -> "aegisctl node restore-log-policy --service " + service;
            case OPEN_INCIDENT -> "aegisctl incident annotate --service " + service + " --note rollback-not-required";
        };
    }

    private static String burnRateLabel(SloBurnRate burnRate) {
        if (burnRate == null) {
            return "unknown";
        }
        return Math.round(burnRate.burnRate() * 10.0) / 10.0 + "x";
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, Math.round(value * 100.0) / 100.0));
    }

    private static double clampPercent(double value) {
        return Math.max(0.0, Math.min(100.0, Math.round(value * 100.0) / 100.0));
    }

    private static final class RemediationPlanBuilder {
        private final java.util.ArrayList<RemediationStep> steps = new java.util.ArrayList<>();

        void add(
                AutopilotPhase phase,
                String title,
                String commandPreview,
                String successCriteria,
                String rollbackTrigger,
                int waitSeconds,
                double blastRadiusPercent
        ) {
            steps.add(new RemediationStep(
                    steps.size() + 1,
                    phase,
                    title,
                    commandPreview,
                    successCriteria,
                    rollbackTrigger,
                    waitSeconds,
                    blastRadiusPercent
            ));
        }

        List<RemediationStep> steps() {
            return List.copyOf(steps);
        }
    }
}
