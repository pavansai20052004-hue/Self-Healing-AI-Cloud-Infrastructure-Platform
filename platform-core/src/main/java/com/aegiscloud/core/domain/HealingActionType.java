package com.aegiscloud.core.domain;

public enum HealingActionType {
    SCALE_OUT,
    RESTART_SERVICE,
    ROLLBACK_DEPLOYMENT,
    CLEAR_QUEUE,
    INCREASE_MEMORY_LIMIT,
    THROTTLE_TRAFFIC,
    PRUNE_LOGS,
    OPEN_INCIDENT
}

