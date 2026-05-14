package com.aegiscloud.core.demo;

import com.aegiscloud.core.detect.AnomalyDetector;
import com.aegiscloud.core.domain.HealingDecision;
import com.aegiscloud.core.domain.MetricSample;
import com.aegiscloud.core.domain.Prediction;
import com.aegiscloud.core.domain.ServiceState;
import com.aegiscloud.core.heal.HealingPolicyEngine;
import com.aegiscloud.core.incident.IncidentReportGenerator;

import java.time.Instant;
import java.util.List;

public final class DemoScenario {
    private DemoScenario() {
    }

    public static void main(String[] args) {
        String service = args.length > 0 ? args[0] : "payment-service";
        Instant now = Instant.now();
        List<MetricSample> samples = List.of(
                new MetricSample(service, now.minusSeconds(240), 46, 52, 118, 0.004, 41, 2),
                new MetricSample(service, now.minusSeconds(180), 61, 64, 145, 0.006, 42, 2),
                new MetricSample(service, now.minusSeconds(120), 78, 77, 310, 0.012, 44, 2),
                new MetricSample(service, now.minusSeconds(60), 91, 86, 740, 0.038, 47, 2),
                new MetricSample(service, now, 97, 93, 1_240, 0.092, 48, 2)
        );

        AnomalyDetector detector = new AnomalyDetector();
        HealingPolicyEngine policyEngine = new HealingPolicyEngine();
        IncidentReportGenerator reportGenerator = new IncidentReportGenerator();
        ServiceState state = new ServiceState(service, 2, 1, 6, 12, 1, 1_850, "v2.4.1", "v2.4.0");

        List<Prediction> predictions = detector.detect(samples);
        System.out.println("AegisCloud Self-Healing Demo");
        System.out.println("============================");
        System.out.println("Scenario: " + service + " is under load after a recent deployment.");
        System.out.println("Predictions found: " + predictions.size());
        System.out.println();

        for (Prediction prediction : predictions) {
            HealingDecision decision = policyEngine.decide(prediction, state, true);
            System.out.println("Prediction: " + prediction.type() + " / " + prediction.severity());
            System.out.println("Summary: " + prediction.summary());
            System.out.println("Selected action: " + decision.actions().getFirst().type());
            System.out.println("Dry-run command: " + decision.actions().getFirst().commandPreview());
            System.out.println();

            String report = reportGenerator.generate(
                    prediction,
                    decision,
                    List.of(
                            "WARN pool-12 Connection checkout took 820ms",
                            "ERROR 5xx spike detected on /payments/charge",
                            "INFO deployment v2.4.1 completed 12 minutes ago",
                            "WARN heap usage crossed 90 percent"
                    )
            );
            System.out.println(report);
            System.out.println("---");
        }
    }
}

