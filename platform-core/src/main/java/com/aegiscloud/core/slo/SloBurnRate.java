package com.aegiscloud.core.slo;

public record SloBurnRate(
        String serviceName,
        double burnRate,
        double budgetRemainingPercent,
        SloStatus status,
        String recommendation
) {
    public SloBurnRate {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName is required");
        }
        serviceName = serviceName.trim();
        burnRate = Math.max(0.0, burnRate);
        budgetRemainingPercent = Math.max(0.0, Math.min(100.0, budgetRemainingPercent));
        status = status == null ? SloStatus.HEALTHY : status;
        recommendation = recommendation == null || recommendation.isBlank()
                ? "No SLO recommendation generated."
                : recommendation.trim();
    }

    public boolean isUrgent() {
        return status == SloStatus.BURNING || status == SloStatus.EXHAUSTED;
    }
}

