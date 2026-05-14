package com.aegiscloud.core.domain;

import java.time.Instant;
import java.util.Objects;

public record MetricSample(
        String serviceName,
        Instant timestamp,
        double cpuUsage,
        double memoryUsage,
        double latencyMs,
        double errorRate,
        double diskUsage,
        int replicas
) {
    public MetricSample {
        serviceName = requireText(serviceName, "serviceName");
        timestamp = Objects.requireNonNullElse(timestamp, Instant.now());
        cpuUsage = percent(cpuUsage);
        memoryUsage = percent(memoryUsage);
        latencyMs = Math.max(0.0, latencyMs);
        errorRate = bounded(errorRate, 0.0, 1.0);
        diskUsage = percent(diskUsage);
        replicas = Math.max(0, replicas);
    }

    public boolean isCritical() {
        return cpuUsage >= 95.0 || memoryUsage >= 94.0 || latencyMs >= 1_500.0 || errorRate >= 0.10 || diskUsage >= 95.0;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static double percent(double value) {
        return bounded(value, 0.0, 100.0);
    }

    private static double bounded(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}

