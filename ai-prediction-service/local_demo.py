from app.engine import classify_log_line, predict_failure


samples = [
    {"cpuUsage": 48, "memoryUsage": 52, "latencyMs": 110, "errorRate": 0.004, "diskUsage": 36},
    {"cpuUsage": 65, "memoryUsage": 66, "latencyMs": 180, "errorRate": 0.008, "diskUsage": 38},
    {"cpuUsage": 81, "memoryUsage": 79, "latencyMs": 430, "errorRate": 0.018, "diskUsage": 40},
    {"cpuUsage": 94, "memoryUsage": 91, "latencyMs": 920, "errorRate": 0.061, "diskUsage": 44},
]

prediction = predict_failure("payment-service", samples)
print(prediction)
print(classify_log_line("ERROR payment-service 5xx spike after rollout v2.4.1"))

