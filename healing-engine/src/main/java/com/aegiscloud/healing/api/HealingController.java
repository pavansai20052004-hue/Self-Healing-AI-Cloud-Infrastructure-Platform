package com.aegiscloud.healing.api;

import com.aegiscloud.core.autopilot.CounterfactualRemediationPlan;
import com.aegiscloud.core.autopilot.CounterfactualRemediationPlanner;
import com.aegiscloud.core.domain.HealingActionType;
import com.aegiscloud.core.domain.HealingDecision;
import com.aegiscloud.core.domain.Prediction;
import com.aegiscloud.core.domain.ServiceState;
import com.aegiscloud.core.governance.HealingGuardrailEngine;
import com.aegiscloud.core.governance.TenantProfile;
import com.aegiscloud.core.heal.HealingPolicyEngine;
import com.aegiscloud.core.slo.SloBurnRate;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
@RequestMapping("/api/v1/heal")
public class HealingController {
    private final HealingPolicyEngine policyEngine = new HealingPolicyEngine();
    private final HealingGuardrailEngine guardrailEngine = new HealingGuardrailEngine();
    private final CounterfactualRemediationPlanner autopilotPlanner = new CounterfactualRemediationPlanner();

    @GetMapping("/healthz")
    public Map<String, String> health() {
        return Map.of("service", "healing-engine", "status", "ready");
    }

    @PostMapping("/decisions")
    public HealingDecision decide(@Valid @RequestBody HealingRequest request) {
        boolean dryRun = request.dryRun() == null || request.dryRun();
        ServiceState state = request.state() == null
                ? ServiceState.defaultFor(request.prediction().serviceName())
                : request.state();
        return policyEngine.decide(request.prediction(), state, dryRun);
    }

    @PostMapping("/governed-decisions")
    public HealingDecision governedDecision(@Valid @RequestBody GovernedHealingRequest request) {
        boolean dryRun = request.dryRun() != null && request.dryRun();
        ServiceState state = request.state() == null
                ? ServiceState.defaultFor(request.prediction().serviceName())
                : request.state();
        TenantProfile profile = request.tenantProfile() == null
                ? TenantProfile.productionDefault("default", "Default Tenant")
                : request.tenantProfile();
        HealingDecision rawDecision = policyEngine.decide(request.prediction(), state, dryRun);
        return guardrailEngine.apply(rawDecision, state, profile, request.sloBurnRate());
    }

    @PostMapping("/autopilot-plans")
    public CounterfactualRemediationPlan autopilotPlan(@Valid @RequestBody GovernedHealingRequest request) {
        boolean dryRun = request.dryRun() != null && request.dryRun();
        ServiceState state = request.state() == null
                ? ServiceState.defaultFor(request.prediction().serviceName())
                : request.state();
        TenantProfile profile = request.tenantProfile() == null
                ? TenantProfile.productionDefault("default", "Default Tenant")
                : request.tenantProfile();
        HealingDecision rawDecision = policyEngine.decide(request.prediction(), state, dryRun);
        HealingDecision governedDecision = guardrailEngine.apply(rawDecision, state, profile, request.sloBurnRate());
        return autopilotPlanner.plan(governedDecision, state, profile, request.sloBurnRate());
    }

    @PostMapping("/outcomes")
    public Map<String, Object> recordOutcome(@Valid @RequestBody OutcomeRequest request) {
        policyEngine.recordOutcome(request.actionType(), request.recovered(), request.downtimeSeconds());
        return Map.of(
                "updatedAction", request.actionType(),
                "scores", policyEngine.scorer().snapshot()
        );
    }

    @GetMapping("/policy-scores")
    public Map<HealingActionType, Double> policyScores() {
        return policyEngine.scorer().snapshot();
    }

    public record HealingRequest(
            Prediction prediction,
            ServiceState state,
            Boolean dryRun
    ) {
    }

    public record GovernedHealingRequest(
            Prediction prediction,
            ServiceState state,
            TenantProfile tenantProfile,
            SloBurnRate sloBurnRate,
            Boolean dryRun
    ) {
    }

    public record OutcomeRequest(
            HealingActionType actionType,
            boolean recovered,
            int downtimeSeconds
    ) {
    }
}
