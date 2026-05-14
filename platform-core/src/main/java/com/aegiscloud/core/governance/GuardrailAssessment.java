package com.aegiscloud.core.governance;

import com.aegiscloud.core.domain.HealingAction;

import java.time.Instant;
import java.util.Objects;

public record GuardrailAssessment(
        HealingAction action,
        GuardrailVerdict verdict,
        String reason,
        Instant evaluatedAt
) {
    public GuardrailAssessment {
        action = Objects.requireNonNull(action, "action is required");
        verdict = verdict == null ? GuardrailVerdict.BLOCKED : verdict;
        reason = reason == null || reason.isBlank() ? "No guardrail reason supplied." : reason.trim();
        evaluatedAt = Objects.requireNonNullElse(evaluatedAt, Instant.now());
    }

    public boolean canExecute() {
        return verdict == GuardrailVerdict.APPROVED;
    }
}

