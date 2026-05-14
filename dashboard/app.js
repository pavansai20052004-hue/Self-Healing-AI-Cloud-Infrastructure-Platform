const services = [
  { name: "payment-service", cpu: 48, memory: 54, latency: 118, errorRate: 0.4, replicas: 2, status: "healthy" },
  { name: "orders-service", cpu: 37, memory: 45, latency: 92, errorRate: 0.2, replicas: 2, status: "healthy" },
  { name: "inventory-service", cpu: 42, memory: 49, latency: 105, errorRate: 0.3, replicas: 2, status: "healthy" },
  { name: "notification-service", cpu: 31, memory: 41, latency: 82, errorRate: 0.1, replicas: 1, status: "healthy" },
];

let activeIncident = {
  type: "No active incident",
  service: "cluster",
  severity: "resolved",
  summary: "All services are inside their SLO guardrails.",
};

let governance = {
  autonomy: "Supervised",
  burnRate: "0.8x",
  guardrails: "Ready",
};

let policies = [
  { action: "Observe", reason: "No automated remediation required.", risk: "low", verdict: "approved" },
];

let safetyPlan = createSafetyPlan({
  action: "VERIFY",
  gate: "dry-run",
  recommendation: "Cluster is healthy. Keep a counterfactual verification plan armed for the next anomaly window.",
  recoveryProbability: 0.96,
  residualRisk: 0.04,
  blastRadius: 0,
  confidence: 0.88,
});

let timeline = [
  { title: "Cluster baseline captured", detail: "Metrics, logs, policy scores, and tenant guardrails synchronized." },
];

const metricsGrid = document.querySelector("#overview");
const incidentSummary = document.querySelector("#incidentSummary");
const policyList = document.querySelector("#policyList");
const timelineList = document.querySelector("#timelineList");
const clusterState = document.querySelector("#clusterState");
const autonomyMode = document.querySelector("#autonomyMode");
const burnRate = document.querySelector("#burnRate");
const guardrailState = document.querySelector("#guardrailState");
const auditCount = document.querySelector("#auditCount");
const planRecovery = document.querySelector("#planRecovery");
const planRisk = document.querySelector("#planRisk");
const planBlast = document.querySelector("#planBlast");
const planConfidence = document.querySelector("#planConfidence");
const autopilotRecommendation = document.querySelector("#autopilotRecommendation");
const autopilotSteps = document.querySelector("#autopilotSteps");
const rollbackTriggers = document.querySelector("#rollbackTriggers");

document.querySelectorAll("[data-scenario]").forEach((button) => {
  button.addEventListener("click", () => injectScenario(button.dataset.scenario));
});

document.querySelector("#healButton").addEventListener("click", healIncident);

function injectScenario(scenario) {
  const service = services[0];
  if (scenario === "cpu") {
    Object.assign(service, { cpu: 98, memory: 72, latency: 520, errorRate: 2.4, status: "degraded" });
    activeIncident = {
      type: "CPU_SATURATION",
      service: service.name,
      severity: "critical",
      summary: "CPU reached 98% and request latency crossed the warning threshold.",
    };
    policies = [
      { action: "SCALE_OUT", reason: "Increase replicas from 2 to 3.", risk: "low", verdict: "approved" },
      { action: "THROTTLE_TRAFFIC", reason: "Protect downstream services during recovery.", risk: "medium", verdict: "requires approval" },
    ];
    governance = { autonomy: "Supervised", burnRate: "11.7x", guardrails: "1 approved" };
    safetyPlan = createSafetyPlan({
      action: "SCALE_OUT",
      gate: "approved",
      recommendation: "Execute SCALE_OUT through a one-cell canary, then verify burn rate drops below 1.0x.",
      recoveryProbability: 0.88,
      residualRisk: 0.18,
      blastRadius: 25,
      confidence: 0.87,
    });
  }

  if (scenario === "memory") {
    Object.assign(service, { cpu: 76, memory: 94, latency: 460, errorRate: 1.8, status: "degraded" });
    activeIncident = {
      type: "MEMORY_LEAK",
      service: service.name,
      severity: "critical",
      summary: "Memory is climbing across the sample window and may exhaust the container.",
    };
    policies = [
      { action: "RESTART_SERVICE", reason: "Rolling restart clears leaked heap.", risk: "medium", verdict: "requires approval" },
      { action: "INCREASE_MEMORY_LIMIT", reason: "Apply after repeated restarts.", risk: "high", verdict: "blocked" },
    ];
    governance = { autonomy: "Supervised", burnRate: "16.4x", guardrails: "Approval gate" };
    safetyPlan = createSafetyPlan({
      action: "RESTART_SERVICE",
      gate: "approval",
      recommendation: "Request approval for RESTART_SERVICE and keep rollback ready if restart count rises.",
      recoveryProbability: 0.76,
      residualRisk: 0.39,
      blastRadius: 34,
      confidence: 0.78,
    });
  }

  if (scenario === "latency") {
    Object.assign(service, { cpu: 84, memory: 68, latency: 1240, errorRate: 3.1, status: "degraded" });
    activeIncident = {
      type: "LATENCY_SPIKE",
      service: service.name,
      severity: "critical",
      summary: "Latency crossed 1.2s while queue depth continued rising.",
    };
    policies = [
      { action: "CLEAR_QUEUE", reason: "Drain stale work older than 10 minutes.", risk: "medium", verdict: "requires approval" },
      { action: "SCALE_OUT", reason: "Absorb the spike with an additional replica.", risk: "low", verdict: "approved" },
    ];
    governance = { autonomy: "Supervised", burnRate: "18.9x", guardrails: "1 approved" };
    safetyPlan = createSafetyPlan({
      action: "SCALE_OUT",
      gate: "approved",
      recommendation: "Prefer the approved SCALE_OUT path first; queue drain remains approval-gated.",
      recoveryProbability: 0.84,
      residualRisk: 0.24,
      blastRadius: 25,
      confidence: 0.83,
    });
  }

  if (scenario === "errors") {
    Object.assign(service, { cpu: 88, memory: 82, latency: 980, errorRate: 9.2, status: "failing" });
    activeIncident = {
      type: "ERROR_RATE",
      service: service.name,
      severity: "critical",
      summary: "5xx rate surged after the latest deployment.",
    };
    policies = [
      { action: "ROLLBACK_DEPLOYMENT", reason: "Rollback v2.4.1 to the last healthy revision.", risk: "high", verdict: "requires approval" },
      { action: "RESTART_SERVICE", reason: "Recover bad local connection state.", risk: "medium", verdict: "requires approval" },
    ];
    governance = { autonomy: "Supervised", burnRate: "29.2x", guardrails: "Approval gate" };
    safetyPlan = createSafetyPlan({
      action: "ROLLBACK_DEPLOYMENT",
      gate: "approval",
      recommendation: "Request rollback approval with deployment evidence and a 62% blast-radius warning.",
      recoveryProbability: 0.82,
      residualRisk: 0.46,
      blastRadius: 62,
      confidence: 0.80,
    });
  }

  timeline.unshift({
    title: `${activeIncident.type} detected`,
    detail: `${activeIncident.service} moved into ${activeIncident.severity} state.`,
  });
  timeline.unshift({
    title: "Guardrails evaluated",
    detail: `${governance.guardrails} under ${governance.autonomy.toLowerCase()} autonomy.`,
  });
  clusterState.textContent = "Incident predicted";
  render();
}

function healIncident() {
  const service = services[0];
  const firstPolicy = policies[0];

  if (!firstPolicy || activeIncident.severity === "resolved") {
    return;
  }

  if (firstPolicy.action === "SCALE_OUT") {
    service.replicas += 1;
  }

  Object.assign(service, {
    cpu: 52,
    memory: 58,
    latency: 140,
    errorRate: 0.5,
    status: "healthy",
  });

  timeline.unshift({
    title: `${firstPolicy.action} executed`,
    detail: `${activeIncident.service} recovered in dry-run simulation.`,
  });

  activeIncident = {
    type: "Incident resolved",
    service: service.name,
    severity: "resolved",
    summary: `${firstPolicy.action} returned service metrics below SLO thresholds.`,
  };
  policies = [
    { action: "VERIFY", reason: "Continue watching the service for 2 minutes.", risk: "low", verdict: "approved" },
  ];
  governance = { autonomy: "Supervised", burnRate: "1.1x", guardrails: "Recovered" };
  safetyPlan = createSafetyPlan({
    action: "VERIFY",
    gate: "dry-run",
    recommendation: "Recovery is verified. Keep the watch window active and feed the policy scorer with the outcome.",
    recoveryProbability: 0.97,
    residualRisk: 0.03,
    blastRadius: 0,
    confidence: 0.91,
  });
  clusterState.textContent = "Autonomous dry-run";
  render();
}

function render() {
  renderMetrics();
  renderGovernance();
  renderIncident();
  renderPolicies();
  renderAutopilot();
  renderTimeline();
}

function renderGovernance() {
  autonomyMode.textContent = governance.autonomy;
  burnRate.textContent = governance.burnRate;
  guardrailState.textContent = governance.guardrails;
  auditCount.textContent = String(timeline.length);
}

function renderMetrics() {
  metricsGrid.innerHTML = services.map((service) => {
    const load = Math.max(service.cpu, service.memory, Math.min(100, service.latency / 12));
    const color = load > 90 || service.errorRate > 8 ? "var(--red)" : load > 75 ? "var(--amber)" : "var(--green)";
    return `
      <article class="metric-card">
        <h3>${service.name}</h3>
        <div class="metric-value"><strong>${Math.round(service.cpu)}%</strong><span>CPU</span></div>
        <div class="meter"><span style="width: ${Math.min(100, load)}%; background: ${color}"></span></div>
        <div class="metric-meta">
          <span>${service.replicas} replicas</span>
          <span>${Math.round(service.latency)} ms</span>
        </div>
      </article>
    `;
  }).join("");
}

function renderIncident() {
  incidentSummary.innerHTML = `
    <div class="diagnosis ${activeIncident.severity}">
      <strong>${activeIncident.type}</strong>
      <span>${activeIncident.service}: ${activeIncident.summary}</span>
    </div>
  `;
}

function renderPolicies() {
  policyList.innerHTML = policies.map((policy, index) => `
    <div class="policy-item">
      <div class="rank">${index + 1}</div>
      <div>
        <strong>${policy.action}</strong>
        <span>${policy.reason}</span>
      </div>
      <span class="verdict ${policy.verdict.includes("approval") ? "requires" : policy.verdict}">${policy.verdict}</span>
      <span class="risk">${policy.risk}</span>
    </div>
  `).join("");
}

function renderAutopilot() {
  planRecovery.textContent = formatPercent(safetyPlan.recoveryProbability);
  planRisk.textContent = formatPercent(safetyPlan.residualRisk);
  planBlast.textContent = `${Math.round(safetyPlan.blastRadius)}%`;
  planConfidence.textContent = formatPercent(safetyPlan.confidence);

  autopilotRecommendation.innerHTML = `
    <strong>${safetyPlan.action}</strong>
    <span>${safetyPlan.recommendation}</span>
  `;

  autopilotSteps.innerHTML = safetyPlan.steps.map((step) => `
    <li>
      <div class="step-phase">${step.phase}</div>
      <strong>${step.title}</strong>
      <span>${step.command}</span>
    </li>
  `).join("");

  rollbackTriggers.innerHTML = `
    <strong>Rollback triggers</strong>
    ${safetyPlan.triggers.map((trigger) => `<span>${trigger}</span>`).join("")}
  `;
}

function renderTimeline() {
  timelineList.innerHTML = timeline.slice(0, 6).map((event) => `
    <li>
      <strong>${event.title}</strong>
      ${event.detail}
    </li>
  `).join("");
}

function createSafetyPlan({
  action,
  gate,
  recommendation,
  recoveryProbability,
  residualRisk,
  blastRadius,
  confidence,
}) {
  const service = services[0].name;
  const steps = [
    {
      phase: "PREFLIGHT",
      title: "Capture live blast-radius snapshot",
      command: `aegisctl preflight capture --service ${service} --window 10m`,
    },
  ];

  if (gate === "approval") {
    steps.push({
      phase: "APPROVAL",
      title: "Request operator approval",
      command: `aegisctl approval request --service ${service} --action ${action}`,
    });
  }

  if (gate === "dry-run") {
    steps.push({
      phase: "CANARY",
      title: "Run dry-run canary",
      command: `aegisctl canary dry-run --service ${service} --action ${action}`,
    });
    steps.push({
      phase: "VERIFY",
      title: "Compare counterfactual recovery window",
      command: `aegisctl verify counterfactual --service ${service} --window 2m`,
    });
  } else {
    steps.push({
      phase: "CANARY",
      title: "Limit action to first recovery cell",
      command: `aegisctl canary apply --service ${service} --action ${action} --max-blast-radius ${Math.min(blastRadius, 25)}%`,
    });
    steps.push({
      phase: "REMEDIATE",
      title: "Execute selected remediation",
      command: commandFor(action, service),
    });
    steps.push({
      phase: "VERIFY",
      title: "Verify SLO recovery",
      command: `aegisctl verify slo --service ${service} --window 2m --target-burn-rate 1.0`,
    });
  }

  return {
    action,
    recommendation,
    recoveryProbability,
    residualRisk,
    blastRadius,
    confidence,
    steps,
    triggers: [
      "Burn rate fails to improve after two verification windows.",
      "p95 latency or 5xx rate worsens during canary.",
      "Deployment revision changes outside the captured preflight envelope.",
    ],
  };
}

function commandFor(action, service) {
  const commands = {
    SCALE_OUT: `kubectl scale deployment/${service} --replicas=3`,
    RESTART_SERVICE: `kubectl rollout restart deployment/${service}`,
    ROLLBACK_DEPLOYMENT: `kubectl rollout undo deployment/${service}`,
    CLEAR_QUEUE: `aegisctl queue drain --service ${service} --max-age 10m`,
    VERIFY: `aegisctl verify slo --service ${service} --window 2m`,
  };
  return commands[action] || `aegisctl remediate --service ${service} --action ${action}`;
}

function formatPercent(value) {
  return `${Math.round(value * 100)}%`;
}

render();
