package com.aegiscloud.core.slo;

import com.aegiscloud.core.domain.MetricSample;

import java.util.List;

public final class SloBurnRateCalculator {
    public SloBurnRate calculate(SloObjective objective, List<MetricSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return new SloBurnRate(
                    objective.serviceName(),
                    0.0,
                    100.0,
                    SloStatus.HEALTHY,
                    "No samples were available, so no burn-rate pressure was detected."
            );
        }

        double avgErrorRate = samples.stream().mapToDouble(MetricSample::errorRate).average().orElse(0.0);
        double latencyViolationRate = samples.stream()
                .filter(sample -> sample.latencyMs() > objective.latencyObjectiveMs())
                .count() / (double) samples.size();
        double compositeBadEventRate = avgErrorRate + (latencyViolationRate * 0.25);
        double burnRate = compositeBadEventRate / Math.max(objective.errorBudgetFraction(), 0.0001);
        double consumed = Math.min(100.0, burnRate * objective.windowMinutes() / 720.0 * 100.0);
        double remaining = 100.0 - consumed;
        SloStatus status = statusFor(burnRate, remaining);

        String recommendation = switch (status) {
            case HEALTHY -> "Stay in observe mode and keep collecting baseline telemetry.";
            case WATCH -> "Keep automation in dry-run and prepare a low-risk mitigation.";
            case BURNING -> "Prioritize low-risk remediation and page the owning team.";
            case EXHAUSTED -> "Freeze risky deploys, escalate immediately, and prefer rollback or failover.";
        };

        return new SloBurnRate(
                objective.serviceName(),
                round(burnRate),
                round(remaining),
                status,
                recommendation
        );
    }

    private static SloStatus statusFor(double burnRate, double remaining) {
        if (remaining <= 5.0 || burnRate >= 36.0) {
            return SloStatus.EXHAUSTED;
        }
        if (burnRate >= 14.4) {
            return SloStatus.BURNING;
        }
        if (burnRate >= 2.0) {
            return SloStatus.WATCH;
        }
        return SloStatus.HEALTHY;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

