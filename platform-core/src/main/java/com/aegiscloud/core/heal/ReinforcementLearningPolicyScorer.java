package com.aegiscloud.core.heal;

import com.aegiscloud.core.domain.HealingActionType;

import java.util.EnumMap;
import java.util.Map;

public final class ReinforcementLearningPolicyScorer {
    private final Map<HealingActionType, Double> qValues = new EnumMap<>(HealingActionType.class);
    private final double learningRate;

    public ReinforcementLearningPolicyScorer() {
        this(0.35);
    }

    public ReinforcementLearningPolicyScorer(double learningRate) {
        this.learningRate = Math.max(0.05, Math.min(0.95, learningRate));
        for (HealingActionType type : HealingActionType.values()) {
            qValues.put(type, 0.50);
        }
        qValues.put(HealingActionType.OPEN_INCIDENT, 0.20);
        qValues.put(HealingActionType.SCALE_OUT, 0.68);
        qValues.put(HealingActionType.ROLLBACK_DEPLOYMENT, 0.62);
    }

    public double score(HealingActionType actionType) {
        return qValues.getOrDefault(actionType, 0.50);
    }

    public void recordOutcome(HealingActionType actionType, boolean recovered, int downtimeSeconds) {
        double current = score(actionType);
        double downtimePenalty = Math.min(0.45, Math.max(0, downtimeSeconds) / 600.0);
        double reward = recovered ? 1.0 - downtimePenalty : 0.05;
        qValues.put(actionType, current + learningRate * (reward - current));
    }

    public Map<HealingActionType, Double> snapshot() {
        return Map.copyOf(qValues);
    }
}

