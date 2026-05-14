package com.aegiscloud.core.domain;

public record ServiceState(
        String serviceName,
        int replicas,
        int minReplicas,
        int maxReplicas,
        int lastDeploymentAgeMinutes,
        int restartCountLastHour,
        int queueDepth,
        String currentVersion,
        String previousVersion
) {
    public ServiceState {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName is required");
        }
        serviceName = serviceName.trim();
        minReplicas = Math.max(1, minReplicas);
        maxReplicas = Math.max(minReplicas, maxReplicas);
        replicas = Math.max(0, Math.min(maxReplicas, replicas));
        lastDeploymentAgeMinutes = Math.max(0, lastDeploymentAgeMinutes);
        restartCountLastHour = Math.max(0, restartCountLastHour);
        queueDepth = Math.max(0, queueDepth);
        currentVersion = currentVersion == null || currentVersion.isBlank() ? "unknown" : currentVersion.trim();
        previousVersion = previousVersion == null || previousVersion.isBlank() ? currentVersion : previousVersion.trim();
    }

    public static ServiceState defaultFor(String serviceName) {
        return new ServiceState(serviceName, 2, 1, 6, 45, 0, 0, "v1.0.0", "v0.9.9");
    }

    public boolean canScaleOut() {
        return replicas < maxReplicas;
    }

    public boolean wasRecentlyDeployed() {
        return lastDeploymentAgeMinutes <= 30;
    }
}

