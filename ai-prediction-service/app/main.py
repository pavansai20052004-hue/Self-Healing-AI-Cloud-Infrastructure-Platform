from __future__ import annotations

from typing import Any

from fastapi import FastAPI
from pydantic import BaseModel, Field

from app.engine import classify_log_line, predict_failure


app = FastAPI(
    title="AegisCloud AI Prediction Service",
    version="0.1.0",
    description="Lightweight anomaly prediction and log classification service for the AegisCloud platform.",
)


class MetricPoint(BaseModel):
    timestamp: str | None = None
    cpuUsage: float = Field(ge=0, le=100)
    memoryUsage: float = Field(ge=0, le=100)
    latencyMs: float = Field(ge=0)
    errorRate: float = Field(ge=0, le=1)
    diskUsage: float = Field(ge=0, le=100)
    replicas: int = Field(default=1, ge=0)


class PredictionRequest(BaseModel):
    serviceName: str
    samples: list[MetricPoint]


class LogRequest(BaseModel):
    line: str


@app.get("/healthz")
def health() -> dict[str, str]:
    return {"service": "ai-prediction-service", "status": "ready"}


@app.post("/predict")
def predict(request: PredictionRequest) -> dict[str, Any]:
    result = predict_failure(
        request.serviceName,
        [sample.model_dump() for sample in request.samples],
    )
    return {
        "serviceName": result.service_name,
        "riskScore": result.risk_score,
        "failureWindowMinutes": result.failure_window_minutes,
        "labels": result.labels,
        "explanation": result.explanation,
    }


@app.post("/classify-log")
def classify_log(request: LogRequest) -> dict[str, Any]:
    return classify_log_line(request.line)

