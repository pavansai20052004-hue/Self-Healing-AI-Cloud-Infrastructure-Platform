package com.aegiscloud.core.incident;

import com.aegiscloud.core.autopilot.ActionImpactEstimate;
import com.aegiscloud.core.autopilot.CounterfactualRemediationPlan;
import com.aegiscloud.core.autopilot.RemediationStep;
import com.aegiscloud.core.domain.HealingAction;
import com.aegiscloud.core.domain.HealingDecision;
import com.aegiscloud.core.domain.Prediction;
import com.aegiscloud.core.governance.GuardrailAssessment;

import java.util.List;

public final class IncidentReportGenerator {
    public String generate(Prediction prediction, HealingDecision decision, List<String> recentLogs) {
        return generate(prediction, decision, null, recentLogs);
    }

    public String generate(
            Prediction prediction,
            HealingDecision decision,
            CounterfactualRemediationPlan plan,
            List<String> recentLogs
    ) {
        StringBuilder report = new StringBuilder();
        report.append("# AegisCloud Incident Report\n\n");
        report.append("Service: ").append(prediction.serviceName()).append("\n");
        report.append("Type: ").append(prediction.type()).append("\n");
        report.append("Severity: ").append(prediction.severity()).append("\n");
        report.append("Confidence: ").append(String.format("%.0f%%", prediction.confidence() * 100.0)).append("\n\n");

        report.append("## Root Cause Hypothesis\n");
        report.append(prediction.summary()).append("\n\n");

        report.append("## Evidence\n");
        for (String item : prediction.evidence()) {
            report.append("- ").append(item).append("\n");
        }
        report.append("\n");

        if (!decision.guardrailAssessments().isEmpty()) {
            report.append("## Governance Guardrails\n");
            for (GuardrailAssessment assessment : decision.guardrailAssessments()) {
                report.append("- ").append(assessment.action().type())
                        .append(": ").append(assessment.verdict())
                        .append(" - ").append(assessment.reason())
                        .append("\n");
            }
            report.append("\n");
        }

        if (plan != null) {
            report.append("## Counterfactual Safety Plan\n");
            report.append("- Recommendation: ").append(plan.recommendation()).append("\n");
            report.append("- Recovery probability: ")
                    .append(String.format("%.0f%%", plan.expectedRecoveryProbability() * 100.0)).append("\n");
            report.append("- Residual risk: ")
                    .append(String.format("%.0f%%", plan.residualRiskScore() * 100.0)).append("\n");
            report.append("- Max blast radius: ")
                    .append(String.format("%.0f%%", plan.maxBlastRadiusPercent())).append("\n");
            report.append("- Confidence: ")
                    .append(String.format("%.0f%%", plan.overallConfidence() * 100.0)).append("\n\n");

            report.append("### Counterfactual Impact Estimates\n");
            for (ActionImpactEstimate estimate : plan.impactEstimates()) {
                report.append("- ").append(estimate.actionType())
                        .append(": recovery ")
                        .append(String.format("%.0f%%", estimate.recoveryProbability() * 100.0))
                        .append(", impact reduction ")
                        .append(String.format("%.0f%%", estimate.userImpactReductionPercent()))
                        .append(", blast radius ")
                        .append(String.format("%.0f%%", estimate.blastRadiusPercent()))
                        .append(", verdict ")
                        .append(estimate.guardrailVerdict())
                        .append("\n");
            }
            report.append("\n");

            report.append("### Execution Steps\n");
            for (RemediationStep step : plan.steps()) {
                report.append(step.stepNumber()).append(". ")
                        .append(step.phase())
                        .append(" - ")
                        .append(step.title())
                        .append("\n");
                report.append("   Command: `").append(step.commandPreview()).append("`\n");
                report.append("   Success: ").append(step.successCriteria()).append("\n");
                report.append("   Rollback trigger: ").append(step.rollbackTrigger()).append("\n");
            }
            report.append("\n");
        }

        report.append("## Auto-Healing Plan\n");
        if (decision.actions().isEmpty()) {
            report.append("- No automated action was selected.\n");
        } else {
            for (HealingAction action : decision.actions()) {
                report.append("- ").append(action.type())
                        .append(" on ").append(action.targetService())
                        .append(" (risk: ").append(action.riskLevel())
                        .append(", eta: ").append(action.estimatedRecoverySeconds()).append("s)\n");
                report.append("  Command: `").append(action.commandPreview()).append("`\n");
                report.append("  Reason: ").append(action.reason()).append("\n");
            }
        }
        report.append("\n");

        report.append("## Log Signals\n");
        List<String> logs = recentLogs == null ? List.of() : recentLogs;
        if (logs.isEmpty()) {
            report.append("- No logs were attached to this report.\n");
        } else {
            logs.stream().limit(6).forEach(log -> report.append("- ").append(log).append("\n"));
        }
        report.append("\n");

        report.append("## Operator Notes\n");
        report.append("- Mode: ").append(decision.executionMode()).append("\n");
        report.append("- Requires approval: ").append(decision.requiresApproval()).append("\n");
        report.append("- Policy decision: ").append(decision.policyExplanation()).append("\n");
        report.append("- Next check: verify metrics return below SLO thresholds within 2 minutes.\n");

        return report.toString();
    }
}
