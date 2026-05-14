package com.aegiscloud.core.slo;

public record SloObjective(
        String serviceName,
        double availabilityTarget,
        double maxErrorRate,
        double latencyObjectiveMs,
        int windowMinutes
) {
    public SloObjective {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName is required");
        }
        serviceName = serviceName.trim();
        availabilityTarget = bounded(availabilityTarget, 0.900, 0.99999);
        maxErrorRate = bounded(maxErrorRate, 0.0001, 0.50);
        latencyObjectiveMs = Math.max(1.0, latencyObjectiveMs);
        windowMinutes = Math.max(1, windowMinutes);
    }

    public double errorBudgetFraction() {
        return 1.0 - availabilityTarget;
    }

    private static double bounded(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}

