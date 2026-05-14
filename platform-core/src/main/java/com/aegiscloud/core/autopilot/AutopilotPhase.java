package com.aegiscloud.core.autopilot;

public enum AutopilotPhase {
    PREFLIGHT,
    APPROVAL_GATE,
    CANARY,
    REMEDIATE,
    VERIFY,
    ROLLBACK_READY,
    ESCALATE
}
