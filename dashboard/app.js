const services = [
  { name: "payment-service", cpu: 48, memory: 54, latency: 118, errorRate: 0.4, replicas: 2, status: "healthy" },
  { name: "orders-service", cpu: 37, memory: 45, latency: 92, errorRate: 0.2, replicas: 2, status: "healthy" },
  { name: "inventory-service", cpu: 42, memory: 49, latency: 105, errorRate: 0.3, replicas: 2, status: "healthy" },
  { name: "notification-service", cpu: 31, memory: 41, latency: 82, errorRate: 0.1, replicas: 1, status: "healthy" },
];

const api = {
  monitoring: "http://localhost:8081/api/v1",
  healing: "http://localhost:8082/api/v1/heal",
  incidents: "http://localhost:8083/api/v1/incidents",
  ai: "http://localhost:8090",
};

const requestTimeoutMs = 3500;

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
  button.addEventListener("click", () => {
    void injectScenario(button.dataset.scenario);
  });
});

document.querySelector("#healButton").addEventListener("click", healIncident);

async function injectScenario(scenario) {
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
  clusterState.textContent = "Incident predicted, querying live APIs";
  render();
  await hydrateFromLiveApis(scenario);
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

async function hydrateFromLiveApis(scenario) {
  const service = services[0];
  const samples = scenarioSamples(scenario);

  try {
    const [analysis, aiPrediction] = await Promise.all([
      postJson(`${api.monitoring}/metrics/analyze`, {
        serviceName: service.name,
        samples,
      }),
      postJson(`${api.ai}/predict`, {
        serviceName: service.name,
        samples,
      }),
    ]);

    const predictions = analysis.predictions || [];
    const leadPrediction = selectLeadPrediction(predictions, scenario);
    if (!leadPrediction) {
      throw new Error("Monitoring API returned no incident prediction.");
    }

    const healingPayload = {
      dryRun: false,
      prediction: leadPrediction,
      state: stateForScenario(scenario),
      tenantProfile: tenantProfile(),
      sloBurnRate: sloBurnRateForScenario(scenario),
    };

    const [decision, plan] = await Promise.all([
      postJson(`${api.healing}/governed-decisions`, healingPayload),
      postJson(`${api.healing}/autopilot-plans`, healingPayload),
    ]);

    activeIncident = {
      type: leadPrediction.type,
      service: leadPrediction.serviceName,
      severity: (leadPrediction.severity || "warning").toLowerCase(),
      summary: `${leadPrediction.summary} AI risk ${Math.round((aiPrediction.riskScore || 0) * 100)}% in ${aiPrediction.failureWindowMinutes || "unknown"} minutes.`,
    };
    policies = policiesFromDecision(decision);
    safetyPlan = safetyPlanFromApi(plan);
    governance = {
      autonomy: "Supervised",
      burnRate: `${sloBurnRateForScenario(scenario).burnRate}x`,
      guardrails: guardrailSummary(decision),
    };
    clusterState.textContent = "Live backend APIs connected";
    timeline.unshift({
      title: "Live backend APIs responded",
      detail: `Monitoring found ${predictions.length} prediction(s), AI labeled ${aiPrediction.labels.join(", ")}, and healing returned ${plan.executionMode}.`,
    });
  } catch (error) {
    clusterState.textContent = "Simulation mode, backend fallback";
    timeline.unshift({
      title: "Backend API fallback",
      detail: `Dashboard kept the local simulation because live API hydration failed: ${error.message}`,
    });
  }
}

async function postJson(url, body) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), requestTimeoutMs);
  try {
    const response = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
      signal: controller.signal,
    });
    if (!response.ok) {
      throw new Error(`${response.status} ${response.statusText}`);
    }
    return await response.json();
  } finally {
    clearTimeout(timeout);
  }
}

function scenarioSamples(scenario) {
  const now = Date.now();
  const minute = 60 * 1000;
  const service = services[0];
  const shape = {
    cpu: [
      [48, 54, 118, 0.004, 42],
      [68, 61, 180, 0.008, 43],
      [82, 67, 310, 0.014, 44],
      [91, 71, 460, 0.021, 45],
      [98, 72, 520, 0.024, 46],
    ],
    memory: [
      [42, 52, 120, 0.004, 42],
      [52, 64, 180, 0.006, 43],
      [64, 76, 260, 0.009, 44],
      [72, 86, 360, 0.014, 45],
      [76, 94, 460, 0.018, 46],
    ],
    latency: [
      [44, 50, 110, 0.004, 42],
      [58, 55, 220, 0.006, 43],
      [69, 60, 540, 0.012, 44],
      [78, 64, 920, 0.022, 45],
      [84, 68, 1240, 0.031, 46],
    ],
    errors: [
      [52, 48, 130, 0.003, 42],
      [68, 57, 210, 0.006, 44],
      [79, 70, 390, 0.014, 46],
      [88, 82, 740, 0.041, 48],
      [94, 90, 980, 0.092, 50],
    ],
  }[scenario] || [];

  return shape.map(([cpuUsage, memoryUsage, latencyMs, errorRate, diskUsage], index) => ({
    timestamp: new Date(now - (shape.length - index - 1) * minute).toISOString(),
    cpuUsage,
    memoryUsage,
    latencyMs,
    errorRate,
    diskUsage,
    replicas: service.replicas,
  }));
}

function selectLeadPrediction(predictions, scenario) {
  const preferred = {
    cpu: "CPU_SATURATION",
    memory: "MEMORY_LEAK",
    latency: "LATENCY_SPIKE",
    errors: "ERROR_RATE",
  }[scenario];
  return predictions.find((prediction) => prediction.type === preferred)
    || predictions.sort((left, right) => right.confidence - left.confidence)[0];
}

function stateForScenario(scenario) {
  const service = services[0];
  const queueDepth = scenario === "latency" || scenario === "errors" ? 2400 : 800;
  return {
    serviceName: service.name,
    replicas: service.replicas,
    minReplicas: 2,
    maxReplicas: 6,
    lastDeploymentAgeMinutes: scenario === "errors" ? 8 : 45,
    restartCountLastHour: scenario === "memory" ? 3 : 1,
    queueDepth,
    currentVersion: "v2.4.1",
    previousVersion: "v2.4.0",
  };
}

function tenantProfile() {
  return {
    tenantId: "fintech-prod",
    displayName: "Northstar Payments",
    environment: "production",
    autonomyMode: "SUPERVISED",
    highRiskApprovalRequired: true,
    maxReplicas: 6,
    maxActionsPerIncident: 2,
    actionCooldownSeconds: 180,
    maxAllowedBurnRate: 14.4,
    allowedActions: [
      "SCALE_OUT",
      "RESTART_SERVICE",
      "ROLLBACK_DEPLOYMENT",
      "CLEAR_QUEUE",
      "THROTTLE_TRAFFIC",
      "PRUNE_LOGS",
      "OPEN_INCIDENT",
    ],
  };
}

function sloBurnRateForScenario(scenario) {
  const burnRates = {
    cpu: 11.7,
    memory: 16.4,
    latency: 18.9,
    errors: 29.2,
  };
  const burnRateValue = burnRates[scenario] || 1.0;
  return {
    serviceName: services[0].name,
    burnRate: burnRateValue,
    budgetRemainingPercent: burnRateValue > 20 ? 12.6 : 28.0,
    status: burnRateValue > 20 ? "BURNING" : "WATCH",
    recommendation: "Prioritize low-risk remediation and page the owning team.",
  };
}

function policiesFromDecision(decision) {
  if (decision.guardrailAssessments && decision.guardrailAssessments.length > 0) {
    return decision.guardrailAssessments.map((assessment) => ({
      action: assessment.action.type,
      reason: assessment.action.reason,
      risk: assessment.action.riskLevel,
      verdict: assessment.verdict.toLowerCase().replaceAll("_", " "),
    }));
  }
  return (decision.actions || []).map((action) => ({
    action: action.type,
    reason: action.reason,
    risk: action.riskLevel,
    verdict: decision.dryRun ? "dry run only" : "approved",
  }));
}

function safetyPlanFromApi(plan) {
  const lead = (plan.impactEstimates || [])[0];
  return {
    action: lead ? lead.actionType : "AUTOPILOT_PLAN",
    recommendation: plan.recommendation,
    recoveryProbability: plan.expectedRecoveryProbability,
    residualRisk: plan.residualRiskScore,
    blastRadius: plan.maxBlastRadiusPercent,
    confidence: plan.overallConfidence,
    steps: (plan.steps || []).map((step) => ({
      phase: step.phase,
      title: step.title,
      command: step.commandPreview,
    })),
    triggers: plan.rollbackTriggers || [],
  };
}

function guardrailSummary(decision) {
  const assessments = decision.guardrailAssessments || [];
  if (assessments.length === 0) {
    return decision.executionMode || "ready";
  }
  const approved = assessments.filter((item) => item.verdict === "APPROVED").length;
  const approvals = assessments.filter((item) => item.verdict === "REQUIRES_APPROVAL").length;
  const blocked = assessments.filter((item) => item.verdict === "BLOCKED").length;
  if (approvals > 0) {
    return `${approvals} approval gate`;
  }
  if (blocked > 0) {
    return `${blocked} blocked`;
  }
  return `${approved} approved`;
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
