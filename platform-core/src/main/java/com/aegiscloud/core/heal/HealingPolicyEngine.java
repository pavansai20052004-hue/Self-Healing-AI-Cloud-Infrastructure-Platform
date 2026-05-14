package com.aegiscloud.core.heal;

import com.aegiscloud.core.domain.HealingAction;
import com.aegiscloud.core.domain.HealingActionType;
import com.aegiscloud.core.domain.HealingDecision;
import com.aegiscloud.core.domain.IncidentType;
import com.aegiscloud.core.domain.Prediction;
import com.aegiscloud.core.domain.ServiceState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class HealingPolicyEngine {
    private final ReinforcementLearningPolicyScorer policyScorer;

    public HealingPolicyEngine() {
        this(new ReinforcementLearningPolicyScorer());
    }

    public HealingPolicyEngine(ReinforcementLearningPolicyScorer policyScorer) {
        this.policyScorer = policyScorer;
    }

    public HealingDecision decide(Prediction prediction, ServiceState state, boolean dryRun) {
        ServiceState safeState = state == null ? ServiceState.defaultFor(prediction.serviceName()) : state;
        List<HealingAction> actions = switch (prediction.type()) {
            case CPU_SATURATION -> cpuActions(prediction, safeState);
            case MEMORY_LEAK -> memoryActions(prediction, safeState);
            case LATENCY_SPIKE -> latencyActions(prediction, safeState);
            case ERROR_RATE -> errorRateActions(prediction, safeState);
            case DISK_PRESSURE -> diskActions(prediction, safeState);
        };

        actions = actions.stream()
                .sorted(Comparator.comparingDouble((HealingAction action) -> policyScorer.score(action.type())).reversed())
                .toList();

        String explanation = "Policy ranked " + actions.size() + " action(s) using incident type, service state, and learned outcome scores.";
        return new HealingDecision(
                "dec-" + UUID.randomUUID(),
                Instant.now(),
                prediction,
                actions,
                dryRun,
                explanation
        );
    }

    public void recordOutcome(HealingActionType actionType, boolean recovered, int downtimeSeconds) {
        policyScorer.recordOutcome(actionType, recovered, downtimeSeconds);
    }

    public ReinforcementLearningPolicyScorer scorer() {
        return policyScorer;
    }

    private static List<HealingAction> cpuActions(Prediction prediction, ServiceState state) {
        List<HealingAction> actions = new ArrayList<>();
        if (state.canScaleOut()) {
            actions.add(action(
                    HealingActionType.SCALE_OUT,
                    prediction.serviceName(),
                    "CPU pressure can be reduced by adding replicas.",
                    "kubectl scale deployment/" + prediction.serviceName() + " --replicas=" + (state.replicas() + 1),
                    "low",
                    40
            ));
        }
        actions.add(action(
                HealingActionType.THROTTLE_TRAFFIC,
                prediction.serviceName(),
                "Protect downstream systems while the cluster recovers.",
                "kubectl annotate service/" + prediction.serviceName() + " aegiscloud.io/throttle=enabled",
                "medium",
                20
        ));
        return actions;
    }

    private static List<HealingAction> memoryActions(Prediction prediction, ServiceState state) {
        List<HealingAction> actions = new ArrayList<>();
        actions.add(action(
                HealingActionType.RESTART_SERVICE,
                prediction.serviceName(),
                "A rolling restart clears leaked heap while keeping healthy replicas available.",
                "kubectl rollout restart deployment/" + prediction.serviceName(),
                "medium",
                55
        ));
        if (state.restartCountLastHour() >= 2) {
            actions.add(action(
                    HealingActionType.INCREASE_MEMORY_LIMIT,
                    prediction.serviceName(),
                    "Repeated restarts suggest the memory limit is too tight for current load.",
                    "kubectl patch deployment/" + prediction.serviceName() + " --type=json -p='[{\"op\":\"replace\",\"path\":\"/spec/template/spec/containers/0/resources/limits/memory\",\"value\":\"768Mi\"}]'",
                    "high",
                    90
            ));
        }
        return actions;
    }

    private static List<HealingAction> latencyActions(Prediction prediction, ServiceState state) {
        List<HealingAction> actions = new ArrayList<>();
        if (state.queueDepth() > 1_000) {
            actions.add(action(
                    HealingActionType.CLEAR_QUEUE,
                    prediction.serviceName(),
                    "Queue depth is high enough to amplify latency.",
                    "aegisctl queue drain --service " + prediction.serviceName() + " --max-age 10m",
                    "medium",
                    30
            ));
        }
        if (state.canScaleOut()) {
            actions.add(action(
                    HealingActionType.SCALE_OUT,
                    prediction.serviceName(),
                    "Additional replicas should absorb the latency spike.",
                    "kubectl scale deployment/" + prediction.serviceName() + " --replicas=" + (state.replicas() + 1),
                    "low",
                    45
            ));
        }
        actions.add(action(
                HealingActionType.OPEN_INCIDENT,
                prediction.serviceName(),
                "Latency issue needs human review if automated actions do not restore the SLO.",
                "aegisctl incident open --service " + prediction.serviceName() + " --type latency",
                "low",
                10
        ));
        return actions;
    }

    private static List<HealingAction> errorRateActions(Prediction prediction, ServiceState state) {
        List<HealingAction> actions = new ArrayList<>();
        if (state.wasRecentlyDeployed()) {
            actions.add(action(
                    HealingActionType.ROLLBACK_DEPLOYMENT,
                    prediction.serviceName(),
                    "Errors started near a fresh deployment, so rollback to " + state.previousVersion() + " is the fastest low-ambiguity fix.",
                    "kubectl rollout undo deployment/" + prediction.serviceName(),
                    "high",
                    65
            ));
        }
        actions.add(action(
                HealingActionType.RESTART_SERVICE,
                prediction.serviceName(),
                "Restarting can recover from connection pool exhaustion or bad local state.",
                "kubectl rollout restart deployment/" + prediction.serviceName(),
                "medium",
                50
        ));
        return actions;
    }

    private static List<HealingAction> diskActions(Prediction prediction, ServiceState state) {
        return List.of(action(
                HealingActionType.PRUNE_LOGS,
                prediction.serviceName(),
                "Disk pressure is commonly caused by runaway logs or temporary files.",
                "aegisctl node prune-logs --service " + prediction.serviceName() + " --older-than 2h",
                "medium",
                35
        ));
    }

    private static HealingAction action(
            HealingActionType type,
            String serviceName,
            String reason,
            String commandPreview,
            String riskLevel,
            int recoverySeconds
    ) {
        return new HealingAction(type, serviceName, reason, commandPreview, riskLevel, recoverySeconds);
    }
}
