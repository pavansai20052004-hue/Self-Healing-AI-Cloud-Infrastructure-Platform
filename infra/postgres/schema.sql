CREATE TABLE tenants (
    tenant_id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    environment TEXT NOT NULL,
    autonomy_mode TEXT NOT NULL,
    high_risk_approval_required BOOLEAN NOT NULL DEFAULT TRUE,
    max_replicas INTEGER NOT NULL DEFAULT 6,
    max_actions_per_incident INTEGER NOT NULL DEFAULT 3,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE services (
    service_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id TEXT NOT NULL REFERENCES tenants(tenant_id),
    service_name TEXT NOT NULL,
    namespace TEXT NOT NULL,
    current_version TEXT NOT NULL,
    previous_version TEXT,
    min_replicas INTEGER NOT NULL DEFAULT 1,
    max_replicas INTEGER NOT NULL DEFAULT 6,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, service_name)
);

CREATE TABLE slo_objectives (
    slo_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id TEXT NOT NULL REFERENCES tenants(tenant_id),
    service_name TEXT NOT NULL,
    availability_target NUMERIC(7, 5) NOT NULL,
    max_error_rate NUMERIC(8, 6) NOT NULL,
    latency_objective_ms INTEGER NOT NULL,
    window_minutes INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE incidents (
    incident_id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL REFERENCES tenants(tenant_id),
    service_name TEXT NOT NULL,
    status TEXT NOT NULL,
    severity TEXT NOT NULL,
    slo_status TEXT,
    burn_rate NUMERIC(10, 2),
    summary TEXT NOT NULL,
    opened_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE predictions (
    prediction_id TEXT PRIMARY KEY,
    incident_id TEXT REFERENCES incidents(incident_id),
    tenant_id TEXT NOT NULL REFERENCES tenants(tenant_id),
    service_name TEXT NOT NULL,
    incident_type TEXT NOT NULL,
    severity TEXT NOT NULL,
    confidence NUMERIC(5, 4) NOT NULL,
    summary TEXT NOT NULL,
    evidence JSONB NOT NULL DEFAULT '[]',
    recommended_action TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE healing_actions (
    action_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id TEXT REFERENCES incidents(incident_id),
    tenant_id TEXT NOT NULL REFERENCES tenants(tenant_id),
    action_type TEXT NOT NULL,
    target_service TEXT NOT NULL,
    risk_level TEXT NOT NULL,
    command_preview TEXT NOT NULL,
    guardrail_verdict TEXT NOT NULL,
    guardrail_reason TEXT NOT NULL,
    estimated_recovery_seconds INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE autopilot_plans (
    plan_id TEXT PRIMARY KEY,
    incident_id TEXT REFERENCES incidents(incident_id),
    tenant_id TEXT NOT NULL REFERENCES tenants(tenant_id),
    service_name TEXT NOT NULL,
    incident_type TEXT NOT NULL,
    execution_mode TEXT NOT NULL,
    recommendation TEXT NOT NULL,
    overall_confidence NUMERIC(5, 4) NOT NULL,
    expected_recovery_probability NUMERIC(5, 4) NOT NULL,
    residual_risk_score NUMERIC(5, 4) NOT NULL,
    max_blast_radius_percent NUMERIC(6, 2) NOT NULL,
    requires_approval BOOLEAN NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE autopilot_action_estimates (
    estimate_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id TEXT NOT NULL REFERENCES autopilot_plans(plan_id) ON DELETE CASCADE,
    action_type TEXT NOT NULL,
    guardrail_verdict TEXT NOT NULL,
    recovery_probability NUMERIC(5, 4) NOT NULL,
    user_impact_reduction_percent NUMERIC(6, 2) NOT NULL,
    blast_radius_percent NUMERIC(6, 2) NOT NULL,
    confidence NUMERIC(5, 4) NOT NULL,
    utility_score NUMERIC(8, 2) NOT NULL,
    rationale TEXT NOT NULL
);

CREATE TABLE autopilot_plan_steps (
    step_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id TEXT NOT NULL REFERENCES autopilot_plans(plan_id) ON DELETE CASCADE,
    step_number INTEGER NOT NULL,
    phase TEXT NOT NULL,
    title TEXT NOT NULL,
    command_preview TEXT NOT NULL,
    success_criteria TEXT NOT NULL,
    rollback_trigger TEXT NOT NULL,
    wait_seconds INTEGER NOT NULL,
    blast_radius_percent NUMERIC(6, 2) NOT NULL,
    UNIQUE (plan_id, step_number)
);

CREATE TABLE policy_outcomes (
    outcome_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id TEXT NOT NULL REFERENCES tenants(tenant_id),
    incident_id TEXT REFERENCES incidents(incident_id),
    action_type TEXT NOT NULL,
    recovered BOOLEAN NOT NULL,
    downtime_seconds INTEGER NOT NULL,
    reward NUMERIC(6, 4) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_events (
    event_id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL REFERENCES tenants(tenant_id),
    actor TEXT NOT NULL,
    event_type TEXT NOT NULL,
    target TEXT NOT NULL,
    reason TEXT NOT NULL,
    attributes JSONB NOT NULL DEFAULT '{}',
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_incidents_tenant_status ON incidents (tenant_id, status);
CREATE INDEX idx_predictions_service_created ON predictions (tenant_id, service_name, created_at DESC);
CREATE INDEX idx_autopilot_plans_incident ON autopilot_plans (incident_id, generated_at DESC);
CREATE INDEX idx_audit_events_tenant_time ON audit_events (tenant_id, occurred_at DESC);
