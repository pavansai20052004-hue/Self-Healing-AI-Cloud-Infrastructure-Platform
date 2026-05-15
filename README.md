# AegisCloud: Self-Healing AI Cloud Infrastructure Platform

AegisCloud is a Java-first AIOps platform that detects infrastructure anomalies, predicts likely failures, selects automated remediation actions, and generates incident intelligence reports.

The project is built to be demo-friendly for interviews: it has an offline Java core demo, Spring Boot microservice wrappers, a FastAPI prediction service, Kubernetes manifests, Prometheus config, and a static operations dashboard.

## What It Demonstrates

- Java 21 backend engineering with a clean domain core.
- Spring Boot microservices for monitoring, healing, and incident reporting.
- AI-style anomaly prediction using metric and log signals.
- Reinforcement-learning-inspired policy scoring for auto-healing actions.
- Multi-tenant autonomy controls, guardrails, approval gates, action budgets, and audit trails.
- Counterfactual autopilot planning with recovery probability, residual risk, blast radius, canary steps, and rollback triggers before execution.
- SLO burn-rate analysis that changes incident urgency and remediation posture.
- Incident lifecycle management from detection through verification or escalation.
- Kubernetes remediation concepts: scale out, rollout restart, rollback, log pruning, and queue draining.
- DevOps scaffolding with Docker Compose, Prometheus, Grafana, Redis, PostgreSQL, and a Kafka-compatible broker.
- Chaos engineering and incident replay concepts.

## Repository Layout

```text
platform-core/                    Dependency-light Java core
monitoring-service/               Spring Boot anomaly analysis API
healing-engine/                   Spring Boot remediation policy API
incident-intelligence-service/    Spring Boot RCA report API
ai-prediction-service/            FastAPI prediction and log classification service
dashboard/                        Static recruiter-friendly console demo
infra/docker/                     Docker Compose stack
infra/k8s/                        Kubernetes manifests and RBAC
infra/postgres/                   Production-oriented schema
infra/prometheus/                 Prometheus scrape config
docs/                             Architecture and interview notes
scripts/                          Local run helpers
```

## Fast Offline Demos

These paths need only Java 21.

```powershell
.\scripts\run-core-demo.ps1
.\scripts\run-enterprise-demo.ps1
```

The core demo shows detect -> heal -> report. The enterprise demo adds SLO burn-rate intelligence, tenant policy, guardrail verdicts, counterfactual autopilot planning, lifecycle transitions, and an audit trail.

## Python Prediction Demo

This path uses only the Python standard library.

```powershell
.\scripts\run-python-demo.ps1
```

## Static Dashboard

Launch the UI from VS Code or PowerShell:

```powershell
.\scripts\start-dashboard.ps1
```

Or open this file in a browser:

```text
dashboard/index.html
```

The dashboard simulates chaos injection, live diagnosis, policy ranking, counterfactual safety planning, self-healing, and incident replay without needing a backend server.

## Spring Boot Services

When Maven dependencies are available:

```powershell
.\scripts\build-maven.ps1
```

Run services individually:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.11"
mvn -pl monitoring-service spring-boot:run
mvn -pl healing-engine spring-boot:run
mvn -pl incident-intelligence-service spring-boot:run
```

Service ports:

- Monitoring Service: `http://localhost:8081`
- Healing Engine: `http://localhost:8082`
- Incident Intelligence: `http://localhost:8083`
- AI Prediction Service: `http://localhost:8090`

## Docker Compose

After Docker is installed:

```powershell
docker compose -f infra/docker/docker-compose.yml up --build
```

This starts the Java services, Python prediction service, PostgreSQL, Redis, Redpanda Kafka API, Prometheus, and Grafana.

## Kubernetes

```powershell
kubectl apply -f infra/k8s/namespace.yaml
kubectl apply -f infra/k8s/
```

The healing engine is intentionally configured for dry-run and supervised remediation first. High-risk actions should require approval before being enabled against a real cluster.

## Demo Story

1. A service receives abnormal traffic after a deployment.
2. Monitoring detects CPU, memory, latency, and error-rate anomalies.
3. The prediction engine classifies likely failure modes.
4. The healing engine ranks actions using service state and learned policy scores.
5. Governance guardrails decide whether actions are approved, dry-run-only, blocked, or approval-gated.
6. The counterfactual autopilot planner simulates recovery probability, residual risk, blast radius, canary steps, and rollback triggers.
7. Incident intelligence generates a root-cause report and next checks.
8. The dashboard replays the incident timeline, safety plan, and audit trail.

## Next Build Milestones

- Add Kafka events between services.
- Persist incidents, metrics, and policy outcomes in PostgreSQL.
- Add RBAC, tenant isolation, and audit logs.
- Connect the healing engine to the Kubernetes Java client.
- Add OpenTelemetry tracing and Grafana dashboards.
- Train a real anomaly model with generated chaos data.
- Store autopilot plan outcomes and compare predicted recovery probability against actual remediation success.

## Best Files To Show Recruiters

- `platform-core/src/main/java/com/aegiscloud/core/governance/HealingGuardrailEngine.java`
- `platform-core/src/main/java/com/aegiscloud/core/autopilot/CounterfactualRemediationPlanner.java`
- `platform-core/src/main/java/com/aegiscloud/core/slo/SloBurnRateCalculator.java`
- `platform-core/src/main/java/com/aegiscloud/core/demo/EnterpriseAutopilotDemo.java`
- `infra/postgres/schema.sql`
- `infra/k8s/healing-rbac.yaml`
- `docs/production-grade-upgrades.md`
