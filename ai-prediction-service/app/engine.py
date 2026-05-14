from __future__ import annotations

from dataclasses import dataclass
from statistics import mean
from typing import Any


@dataclass(frozen=True)
class PredictionResult:
    service_name: str
    risk_score: float
    failure_window_minutes: int
    labels: list[str]
    explanation: str


def predict_failure(service_name: str, samples: list[dict[str, Any]]) -> PredictionResult:
    if not samples:
        return PredictionResult(service_name, 0.0, 60, ["healthy"], "No metric samples were supplied.")

    latest = samples[-1]
    cpu = _number(latest.get("cpuUsage"))
    memory = _number(latest.get("memoryUsage"))
    latency = _number(latest.get("latencyMs"))
    error_rate = _number(latest.get("errorRate"))
    disk = _number(latest.get("diskUsage"))

    memory_slope = _slope(samples, "memoryUsage")
    latency_slope = _slope(samples, "latencyMs")
    risk = 0.0
    labels: list[str] = []

    if cpu >= 85:
        risk += 0.22
        labels.append("cpu_saturation")
    if memory >= 88 or (memory >= 75 and memory_slope >= 8):
        risk += 0.25
        labels.append("memory_leak")
    if latency >= 500 or latency_slope >= 250:
        risk += 0.18
        labels.append("latency_spike")
    if error_rate >= 0.03:
        risk += 0.24
        labels.append("error_rate")
    if disk >= 82:
        risk += 0.11
        labels.append("disk_pressure")

    risk = min(0.99, risk)
    if not labels:
        labels.append("healthy")

    window = 5 if risk >= 0.80 else 15 if risk >= 0.55 else 45
    explanation = _explain(cpu, memory, memory_slope, latency, error_rate, disk, labels)
    return PredictionResult(service_name, round(risk, 3), window, labels, explanation)


def classify_log_line(line: str) -> dict[str, Any]:
    text = line.lower()
    score = 0.0
    labels: list[str] = []

    if any(token in text for token in ["outofmemory", "heap", "memory"]):
        score += 0.35
        labels.append("memory_leak")
    if any(token in text for token in ["timeout", "latency", "slow", "p95"]):
        score += 0.25
        labels.append("latency_spike")
    if any(token in text for token in ["exception", "5xx", "failed", "error"]):
        score += 0.35
        labels.append("error_rate")
    if any(token in text for token in ["disk", "no space", "filesystem"]):
        score += 0.20
        labels.append("disk_pressure")

    return {
        "riskScore": min(0.99, round(score, 3)),
        "labels": labels or ["normal"],
        "signal": "incident" if score >= 0.35 else "noise",
    }


def _slope(samples: list[dict[str, Any]], field: str) -> float:
    if len(samples) < 4:
        return 0.0
    midpoint = len(samples) // 2
    first = mean(_number(sample.get(field)) for sample in samples[:midpoint])
    second = mean(_number(sample.get(field)) for sample in samples[midpoint:])
    return second - first


def _explain(
    cpu: float,
    memory: float,
    memory_slope: float,
    latency: float,
    error_rate: float,
    disk: float,
    labels: list[str],
) -> str:
    if labels == ["healthy"]:
        return "Metrics are inside the configured safety envelope."
    parts = [
        f"cpu={cpu:.1f}%",
        f"memory={memory:.1f}%",
        f"memorySlope={memory_slope:.1f}",
        f"latency={latency:.0f}ms",
        f"errorRate={error_rate * 100:.1f}%",
        f"disk={disk:.1f}%",
    ]
    return "Signals crossed anomaly thresholds: " + ", ".join(parts)


def _number(value: Any) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return 0.0
