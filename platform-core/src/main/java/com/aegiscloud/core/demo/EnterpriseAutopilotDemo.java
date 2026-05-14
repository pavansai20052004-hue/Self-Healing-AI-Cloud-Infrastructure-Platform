package com.aegiscloud.core.demo;

import com.aegiscloud.core.autopilot.CounterfactualRemediationPlan;
import com.aegiscloud.core.autopilot.CounterfactualRemediationPlanner;
import com.aegiscloud.core.autopilot.RemediationStep;
import com.aegiscloud.core.audit.AuditEvent;
import com.aegiscloud.core.audit.AuditLedger;
import com.aegiscloud.core.detect.AnomalyDetector;
import com.aegiscloud.core.domain.HealingDecision;
import com.aegiscloud.core.domain.IncidentType;
import com.aegiscloud.core.domain.MetricSample;
import com.aegiscloud.core.domain.Prediction;
import com.aegiscloud.core.domain.ServiceState;
import com.aegiscloud.core.governance.AutonomyMode;
import com.aegiscloud.core.governance.GuardrailAssessment;
import com.aegiscloud.core.governance.HealingGuardrailEngine;
import com.aegiscloud.core.governance.TenantProfile;
import com.aegiscloud.core.heal.HealingPolicyEngine;
import com.aegiscloud.core.incident.IncidentLifecycleManager;
import com.aegiscloud.core.incident.IncidentRecord;
import com.aegiscloud.core.incident.IncidentReportGenerator;
import com.aegiscloud.core.slo.SloBurnRate;
import com.aegiscloud.core.slo.SloBurnRateCalculator;
import com.aegiscloud.core.slo.SloObjective;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public final class EnterpriseAutopilotDemo {
    private EnterpriseAutopilotDemo() {
    }

    public static void main(String[] args) {
        String tenantId = "fintech-prod";
        String service = "payment-service";
        Instant now = Instant.now();
        List<MetricSample> samples = List.of(
                new MetricSample(service, now.minusSeconds(600), 52, 48, 130, 0.003, 42, 3),
                new MetricSample(service, now.minusSeconds(480), 68, 57, 210, 0.006, 44, 3),
                new MetricSample(service, now.minusSeconds(360), 79, 70, 390, 0.014, 46, 3),
                new MetricSample(service, now.minusSeconds(240), 88, 82, 740, 0.041, 48, 3),
                new MetricSample(service, now.minusSeconds(120), 94, 90, 980, 0.070, 49, 3),
                new MetricSample(service, now, 97, 93, 1_320, 0.112, 50, 3)
        );

        TenantProfile profile = new TenantProfile(
                tenantId,
                "Northstar Payments",
                "production",
                AutonomyMode.SUPERVISED,
                true,
                6,
                2,
                180,
                14.4,
                TenantProfile.productionDefault(tenantId, "Northstar Payments").allowedActions()
        );

        ServiceState state = new ServiceState(service, 3, 2, 6, 8, 2, 2_400, "v2.4.1", "v2.4.0");
        SloObjective objective = new SloObjective(service, 0.999, 0.01, 350, 30);

        AnomalyDetector detector = new AnomalyDetector();
        HealingPolicyEngine policyEngine = new HealingPolicyEngine();
        HealingGuardrailEngine guardrails = new HealingGuardrailEngine();
        CounterfactualRemediationPlanner autopilotPlanner = new CounterfactualRemediationPlanner();
        SloBurnRateCalculator burnRateCalculator = new SloBurnRateCalculator();
        IncidentLifecycleManager lifecycle = new IncidentLifecycleManager();
        IncidentReportGenerator reportGenerator = new IncidentReportGenerator();
        AuditLedger auditLedger = new AuditLedger();

        List<Prediction> predictions = detector.detect(samples);
        Prediction leadPrediction = predictions.stream()
                .filter(prediction -> prediction.type() == IncidentType.ERROR_RATE)
                .findFirst()
                .orElseGet(() -> predictions.stream()
                        .max(Comparator.comparingDouble(Prediction::confidence))
                        .orElseThrow());
        SloBurnRate burnRate = burnRateCalculator.calculate(objective, samples);
        HealingDecision rawDecision = policyEngine.decide(leadPrediction, state, false);
        HealingDecision governedDecision = guardrails.apply(rawDecision, state, profile, burnRate);
        CounterfactualRemediationPlan safetyPlan = autopilotPlanner.plan(governedDecision, state, profile, burnRate);
        IncidentRecord incident = lifecycle.open(tenantId, service, predictions, governedDecision, burnRate);
        IncidentRecord triaged = lifecycle.afterGuardrails(incident);

        auditLedger.append(AuditEvent.system(tenantId, "incident.detected", service, incident.summary()));
        auditLedger.append(AuditEvent.system(tenantId, "slo.burn_rate.evaluated", service, burnRate.recommendation()));
        auditLedger.append(AuditEvent.system(
                tenantId,
                "autopilot.plan.generated",
                safetyPlan.planId(),
                safetyPlan.recommendation()
        ));
        for (GuardrailAssessment assessment : governedDecision.guardrailAssessments()) {
            auditLedger.append(AuditEvent.system(
                    tenantId,
                    "guardrail." + assessment.verdict().name().toLowerCase(),
                    assessment.action().type().name(),
                    assessment.reason()
            ));
        }
        auditLedger.append(AuditEvent.system(tenantId, "incident.transitioned", triaged.incidentId(), triaged.summary()));

        System.out.println("AegisCloud Enterprise Autopilot Demo");
        System.out.println("====================================");
        System.out.println("Tenant: " + profile.displayName() + " (" + profile.tenantId() + ")");
        System.out.println("Environment: " + profile.environment());
        System.out.println("Autonomy mode: " + profile.autonomyMode());
        System.out.println("Service: " + service + " " + state.currentVersion() + " -> previous " + state.previousVersion());
        System.out.println();

        System.out.println("SLO Intelligence");
        System.out.println("- Burn rate: " + burnRate.burnRate() + "x");
        System.out.println("- Budget remaining: " + burnRate.budgetRemainingPercent() + "%");
        System.out.println("- Status: " + burnRate.status());
        System.out.println("- Recommendation: " + burnRate.recommendation());
        System.out.println();

        System.out.println("Predictions");
        for (Prediction prediction : predictions) {
            System.out.println("- " + prediction.type() + " / " + prediction.severity()
                    + " / confidence " + Math.round(prediction.confidence() * 100.0) + "%");
        }
        System.out.println();

        System.out.println("Guardrail Verdicts");
        for (GuardrailAssessment assessment : governedDecision.guardrailAssessments()) {
            System.out.println("- " + assessment.action().type() + ": " + assessment.verdict());
            System.out.println("  " + assessment.reason());
            System.out.println("  " + assessment.action().commandPreview());
        }
        System.out.println();

        System.out.println("Counterfactual Autopilot Plan");
        System.out.println("- Recommendation: " + safetyPlan.recommendation());
        System.out.println("- Recovery probability: " + Math.round(safetyPlan.expectedRecoveryProbability() * 100.0) + "%");
        System.out.println("- Residual risk: " + Math.round(safetyPlan.residualRiskScore() * 100.0) + "%");
        System.out.println("- Max blast radius: " + Math.round(safetyPlan.maxBlastRadiusPercent()) + "%");
        for (RemediationStep step : safetyPlan.steps()) {
            System.out.println("- Step " + step.stepNumber() + " [" + step.phase() + "]: " + step.title());
            System.out.println("  " + step.commandPreview());
        }
        System.out.println();

        System.out.println("Incident Lifecycle");
        System.out.println("- " + incident.status() + ": " + incident.summary());
        System.out.println("- " + triaged.status() + ": " + triaged.summary());
        System.out.println();

        System.out.println("Audit Trail");
        for (AuditEvent event : auditLedger.events()) {
            System.out.println("- " + event.occurredAt() + " " + event.eventType()
                    + " target=" + event.target());
        }
        System.out.println();

        System.out.println(reportGenerator.generate(
                leadPrediction,
                governedDecision,
                safetyPlan,
                List.of(
                        "ERROR payment-service 5xx spike after rollout v2.4.1",
                        "WARN p95 latency crossed 1.3s for /payments/charge",
                        "WARN heap usage rose from 48% to 93% in 10 minutes",
                        "INFO deployment v2.4.1 completed 8 minutes ago",
                        "WARN queue depth reached 2400 pending payment events"
                )
        ));
    }
}
