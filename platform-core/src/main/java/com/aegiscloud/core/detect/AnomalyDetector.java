package com.aegiscloud.core.detect;

import com.aegiscloud.core.domain.HealingActionType;
import com.aegiscloud.core.domain.IncidentSeverity;
import com.aegiscloud.core.domain.IncidentType;
import com.aegiscloud.core.domain.MetricSample;
import com.aegiscloud.core.domain.Prediction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AnomalyDetector {
    public List<Prediction> detect(List<MetricSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return List.of();
        }

        List<MetricSample> ordered = samples.stream()
                .sorted((left, right) -> left.timestamp().compareTo(right.timestamp()))
                .toList();
        MetricSample latest = ordered.getLast();
        List<Prediction> predictions = new ArrayList<>();

        if (latest.cpuUsage() >= 85.0) {
            predictions.add(prediction(
                    latest,
                    IncidentType.CPU_SATURATION,
                    latest.cpuUsage() >= 95.0 ? IncidentSeverity.CRITICAL : IncidentSeverity.WARNING,
                    latest.cpuUsage() >= 95.0 ? 0.96 : 0.82,
                    "CPU saturation is likely to reduce request throughput.",
                    List.of(
                            "Latest CPU usage: " + pct(latest.cpuUsage()),
                            "Active replicas: " + latest.replicas(),
                            "Observed at: " + latest.timestamp()
                    ),
                    HealingActionType.SCALE_OUT
            ));
        }

        if (hasMemoryLeakShape(ordered)) {
            predictions.add(prediction(
                    latest,
                    IncidentType.MEMORY_LEAK,
                    latest.memoryUsage() >= 92.0 ? IncidentSeverity.CRITICAL : IncidentSeverity.WARNING,
                    latest.memoryUsage() >= 92.0 ? 0.91 : 0.78,
                    "Memory usage is rising across the sample window and may exhaust the container.",
                    List.of(
                            "First memory sample: " + pct(ordered.getFirst().memoryUsage()),
                            "Latest memory sample: " + pct(latest.memoryUsage()),
                            "Window size: " + ordered.size() + " samples"
                    ),
                    HealingActionType.RESTART_SERVICE
            ));
        }

        if (latest.latencyMs() >= 500.0) {
            predictions.add(prediction(
                    latest,
                    IncidentType.LATENCY_SPIKE,
                    latest.latencyMs() >= 1_000.0 ? IncidentSeverity.CRITICAL : IncidentSeverity.WARNING,
                    latest.latencyMs() >= 1_000.0 ? 0.89 : 0.75,
                    "Request latency is above the service objective.",
                    List.of(
                            "Latest latency: " + Math.round(latest.latencyMs()) + " ms",
                            "Error rate at same time: " + pct(latest.errorRate() * 100.0)
                    ),
                    HealingActionType.SCALE_OUT
            ));
        }

        if (latest.errorRate() >= 0.03) {
            predictions.add(prediction(
                    latest,
                    IncidentType.ERROR_RATE,
                    latest.errorRate() >= 0.08 ? IncidentSeverity.CRITICAL : IncidentSeverity.WARNING,
                    latest.errorRate() >= 0.08 ? 0.93 : 0.80,
                    "Error rate indicates active user impact.",
                    List.of(
                            "Latest error rate: " + pct(latest.errorRate() * 100.0),
                            "CPU at same time: " + pct(latest.cpuUsage()),
                            "Latency at same time: " + Math.round(latest.latencyMs()) + " ms"
                    ),
                    HealingActionType.ROLLBACK_DEPLOYMENT
            ));
        }

        if (latest.diskUsage() >= 80.0) {
            predictions.add(prediction(
                    latest,
                    IncidentType.DISK_PRESSURE,
                    latest.diskUsage() >= 92.0 ? IncidentSeverity.CRITICAL : IncidentSeverity.WARNING,
                    latest.diskUsage() >= 92.0 ? 0.88 : 0.73,
                    "Disk pressure can block writes and crash stateful services.",
                    List.of(
                            "Latest disk usage: " + pct(latest.diskUsage()),
                            "Service: " + latest.serviceName()
                    ),
                    HealingActionType.PRUNE_LOGS
            ));
        }

        return predictions;
    }

    private static Prediction prediction(
            MetricSample sample,
            IncidentType type,
            IncidentSeverity severity,
            double confidence,
            String summary,
            List<String> evidence,
            HealingActionType recommendedAction
    ) {
        return new Prediction(
                "pred-" + UUID.randomUUID(),
                sample.serviceName(),
                type,
                severity,
                confidence,
                summary,
                evidence,
                recommendedAction,
                Instant.now()
        );
    }

    private static boolean hasMemoryLeakShape(List<MetricSample> ordered) {
        MetricSample latest = ordered.getLast();
        if (latest.memoryUsage() >= 88.0) {
            return true;
        }
        if (ordered.size() < 4 || latest.memoryUsage() < 75.0) {
            return false;
        }

        int midpoint = ordered.size() / 2;
        double firstHalf = ordered.subList(0, midpoint).stream()
                .mapToDouble(MetricSample::memoryUsage)
                .average()
                .orElse(0.0);
        double secondHalf = ordered.subList(midpoint, ordered.size()).stream()
                .mapToDouble(MetricSample::memoryUsage)
                .average()
                .orElse(0.0);
        return secondHalf - firstHalf >= 12.0;
    }

    private static String pct(double value) {
        return String.format("%.1f%%", value);
    }
}

