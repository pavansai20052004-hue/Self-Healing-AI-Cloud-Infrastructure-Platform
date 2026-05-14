package com.aegiscloud.core.domain;

import com.aegiscloud.core.governance.GuardrailAssessment;
import com.aegiscloud.core.governance.GuardrailVerdict;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record HealingDecision(
        String decisionId,
        Instant createdAt,
        Prediction prediction,
        List<HealingAction> actions,
        boolean dryRun,
        String policyExplanation,
        List<GuardrailAssessment> guardrailAssessments,
        String executionMode,
        boolean requiresApproval
) {
    public HealingDecision(
            String decisionId,
            Instant createdAt,
            Prediction prediction,
            List<HealingAction> actions,
            boolean dryRun,
            String policyExplanation
    ) {
        this(decisionId, createdAt, prediction, actions, dryRun, policyExplanation, List.of(), dryRun ? "dry-run" : "execute", false);
    }

    public HealingDecision {
        if (decisionId == null || decisionId.isBlank()) {
            throw new IllegalArgumentException("decisionId is required");
        }
        createdAt = Objects.requireNonNullElse(createdAt, Instant.now());
        prediction = Objects.requireNonNull(prediction, "prediction is required");
        actions = actions == null ? List.of() : List.copyOf(actions);
        policyExplanation = policyExplanation == null || policyExplanation.isBlank()
                ? "No policy explanation was generated."
                : policyExplanation.trim();
        guardrailAssessments = guardrailAssessments == null ? List.of() : List.copyOf(guardrailAssessments);
        executionMode = executionMode == null || executionMode.isBlank()
                ? (dryRun ? "dry-run" : "execute")
                : executionMode.trim();
        requiresApproval = requiresApproval
                || guardrailAssessments.stream().anyMatch(item -> item.verdict() == GuardrailVerdict.REQUIRES_APPROVAL);
    }

    public boolean hasExecutableAction() {
        return guardrailAssessments.stream().anyMatch(GuardrailAssessment::canExecute);
    }

    public List<HealingAction> executableActions() {
        return guardrailAssessments.stream()
                .filter(GuardrailAssessment::canExecute)
                .map(GuardrailAssessment::action)
                .toList();
    }
}
