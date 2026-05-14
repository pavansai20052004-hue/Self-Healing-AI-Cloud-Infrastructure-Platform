package com.aegiscloud.monitoring.api;

import com.aegiscloud.core.detect.AnomalyDetector;
import com.aegiscloud.core.domain.MetricSample;
import com.aegiscloud.core.domain.Prediction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class MonitoringController {
    private final AnomalyDetector detector = new AnomalyDetector();

    @GetMapping("/healthz")
    public Map<String, String> health() {
        return Map.of("service", "monitoring-service", "status", "ready");
    }

    @PostMapping("/metrics/analyze")
    public AnalysisResponse analyze(@Valid @RequestBody MetricIngestRequest request) {
        List<MetricSample> samples = request.samples().stream()
                .map(point -> point.toSample(request.serviceName()))
                .toList();
        List<Prediction> predictions = detector.detect(samples);
        return new AnalysisResponse(request.serviceName(), samples.size(), predictions);
    }

    @GetMapping("/demo/anomaly")
    public AnalysisResponse demoAnomaly() {
        String serviceName = "payment-service";
        Instant now = Instant.now();
        MetricIngestRequest request = new MetricIngestRequest(serviceName, List.of(
                new MetricPoint(now.minusSeconds(240), 44, 50, 120, 0.004, 36, 2),
                new MetricPoint(now.minusSeconds(180), 62, 65, 180, 0.006, 38, 2),
                new MetricPoint(now.minusSeconds(120), 79, 78, 420, 0.018, 40, 2),
                new MetricPoint(now.minusSeconds(60), 90, 88, 760, 0.044, 42, 2),
                new MetricPoint(now, 98, 94, 1_260, 0.087, 44, 2)
        ));
        return analyze(request);
    }

    public record MetricIngestRequest(
            @NotBlank String serviceName,
            List<MetricPoint> samples
    ) {
        public MetricIngestRequest {
            samples = samples == null ? List.of() : List.copyOf(samples);
        }
    }

    public record MetricPoint(
            Instant timestamp,
            double cpuUsage,
            double memoryUsage,
            double latencyMs,
            double errorRate,
            double diskUsage,
            Integer replicas
    ) {
        MetricSample toSample(String serviceName) {
            return new MetricSample(
                    serviceName,
                    timestamp == null ? Instant.now() : timestamp,
                    cpuUsage,
                    memoryUsage,
                    latencyMs,
                    errorRate,
                    diskUsage,
                    replicas == null ? 1 : replicas
            );
        }
    }

    public record AnalysisResponse(
            String serviceName,
            int samplesAnalyzed,
            List<Prediction> predictions
    ) {
    }
}

