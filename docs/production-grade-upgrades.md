# Production-Grade Upgrade Notes

This project now includes the platform controls expected from a serious self-healing infrastructure system.

## Trust Layer

- `TenantProfile` controls autonomy mode, action budgets, replica limits, and allowed actions.
- `HealingGuardrailEngine` evaluates every remediation before execution.
- High-risk actions such as rollback and memory limit changes can require approval.
- Dry-run and observe-only modes make it safe to demo without touching a real cluster.

## Reliability Intelligence

- `SloBurnRateCalculator` converts metric samples into burn-rate status.
- SLO status influences escalation and remediation urgency.
- Incident lifecycle moves through detected, triaged, mitigating, verifying, resolved, or escalated.

## Counterfactual Autopilot

- `CounterfactualRemediationPlanner` simulates the likely impact of a remediation before mutation.
- Every plan includes recovery probability, residual risk, blast radius, impact estimates, canary steps, rollback triggers, and operator prompts.
- Approval-gated actions now carry evidence that an on-call engineer can use to accept or reject the action quickly.
- The dashboard surfaces the same safety plan so the demo shows explainable autonomy instead of blind automation.

## Auditability

- Every detection, guardrail verdict, transition, and outcome can become an `AuditEvent`.
- The SQL schema includes tables for tenants, services, incidents, predictions, healing actions, outcomes, and audit events.
- Event contracts define how services would communicate through Kafka or Redpanda.
- Autopilot plan tables store the pre-execution forecast, action estimates, and step-by-step command plan.

## Kubernetes Hardening

- Namespace-scoped RBAC for the healing engine.
- Default-deny network policy plus explicit service communication.
- Horizontal Pod Autoscalers for platform services.
- PodDisruptionBudgets for availability during node maintenance.

## Why This Is Interview-Strong

The project no longer says only “I can restart a pod.” It says:

- I understand safe autonomy.
- I understand SLO-driven operations.
- I understand counterfactual planning and blast-radius management.
- I understand audit and compliance needs.
- I understand Kubernetes blast-radius control.
- I can design a system that heals infrastructure without becoming a new outage source.
