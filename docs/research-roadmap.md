# Research Roadmap

## Phase 1: Heuristic Baseline

- Threshold and trend-based anomaly detection.
- Rule-based healing policies.
- Dry-run Kubernetes commands.
- Incident report generation from structured evidence.

## Phase 2: Learning Policies

- Store every healing action and outcome.
- Reward fast recovery and penalize downtime, repeated incidents, and unsafe actions.
- Train action scores per service and per incident type.
- Add cooldown-aware exploration so the system tries better policies without causing instability.

## Phase 3: Predictive Failure Engine

- Generate chaos data from CPU, memory, network, and dependency failures.
- Train models for memory leak prediction, overload detection, and bad deployment classification.
- Compare heuristic, tree-based, and sequence models.
- Report precision, recall, mean time to prediction, and false action rate.

## Phase 4: Human-in-the-Loop Autonomy

- Approval workflow for high-risk actions.
- Operator feedback becomes policy training data.
- Confidence thresholds change by service criticality and tenant policy.
- Reports include uncertainty and alternative explanations.

