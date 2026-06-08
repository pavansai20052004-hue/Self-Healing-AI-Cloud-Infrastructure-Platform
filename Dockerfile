FROM eclipse-temurin:21-jdk-jammy

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        bash \
        ca-certificates \
        curl \
        maven \
        python3 \
        python3-pip \
        python3-venv \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

COPY . .

RUN mvn -pl monitoring-service,healing-engine,incident-intelligence-service -am package -DskipTests
RUN python3 -m venv /opt/ai-venv \
    && /opt/ai-venv/bin/pip install --no-cache-dir -r ai-prediction-service/requirements.txt

COPY render-entrypoint.sh /usr/local/bin/render-entrypoint.sh
RUN chmod +x /usr/local/bin/render-entrypoint.sh

ENV PYTHONPATH=/workspace/ai-prediction-service

ENTRYPOINT ["/usr/local/bin/render-entrypoint.sh"]
