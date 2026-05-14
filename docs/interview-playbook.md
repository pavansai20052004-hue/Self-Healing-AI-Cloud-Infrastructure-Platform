# Interview Playbook

## 30-Second Pitch

AegisCloud is a self-healing AIOps platform built with Java microservices. It monitors service health, predicts failures from metrics and logs, calculates SLO burn rate, ranks remediation actions, evaluates governance guardrails, and generates root-cause reports. I designed the core logic as a dependency-light Java module so it can be tested independently from the cloud runtime.

## Strong Talking Points

- The platform follows a detect -> predict -> heal -> explain loop.
- The core is decoupled from Spring Boot, so business logic is not trapped inside controllers.
- The healing engine uses service state to avoid naive actions. For example, it scales only if replica limits allow it and rolls back only if the deployment is recent.
- Remediation is dry-run first because autonomous infrastructure changes are risky.
- Tenant guardrails decide whether each action is approved, blocked, dry-run-only, or approval-gated.
- SLO burn rate gives the system a production-quality urgency signal instead of relying only on thresholds.
- The audit ledger records detection, guardrail, transition, and outcome events.
- The RL-style policy scorer updates action scores based on recovery outcome and downtime.
- Kubernetes RBAC is intentionally narrow and namespace-scoped.

## Demo Flow

1. Run `.\scripts\run-core-demo.ps1`.
2. Run `.\scripts\run-enterprise-demo.ps1`.
3. Show SLO burn rate, guardrail verdicts, incident lifecycle, and audit events.
4. Explain why the healing engine ranks scale-out, restart, rollback, or queue drain.
5. Open `dashboard/index.html`.
6. Inject a failure, click `Heal`, and show the incident replay.
7. Show `infra/k8s/healing-rbac.yaml` and `infra/k8s/network-policy.yaml` to prove you thought about blast radius.

## Deep Questions To Prepare

- How would you prevent a healing loop from repeatedly restarting a service?
- What actions should require human approval?
- How would you model multi-tenant isolation?
- What metrics prove the platform is improving reliability?
- How would you evaluate the prediction model offline?
- How would this behave during a regional cloud outage?

## Good Answers

- Use cooldown windows, max action budgets, and circuit breakers to prevent runaway healing.
- Require approval for rollback, memory limit changes, data deletion, and any cross-service action.
- Store tenant IDs on every incident, service, metric, and audit event. Enforce authorization before querying or acting.
- Track MTTR, false-positive rate, false-negative rate, action success rate, rollback frequency, and SLO burn-rate reduction.
- Replay historical incidents and chaos-generated incidents to compare predicted labels with known failures.
