package com.aegiscloud.incident.service;

import com.aegiscloud.core.domain.HealingDecision;
import com.aegiscloud.core.domain.Prediction;
import com.aegiscloud.core.incident.IncidentReportGenerator;
import com.aegiscloud.incident.model.IncidentReportSnapshot;
import com.aegiscloud.incident.model.StoredIncidentReport;
import com.aegiscloud.incident.store.IncidentReportStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class IncidentReportService {
    private final IncidentReportGenerator generator = new IncidentReportGenerator();
    private final IncidentReportStore store;
    private final ObjectMapper objectMapper;

    public IncidentReportService(IncidentReportStore store, ObjectMapper objectMapper) {
        this.store = store;
        this.objectMapper = objectMapper;
    }

    public StoredIncidentReport archive(Prediction prediction, HealingDecision decision, List<String> recentLogs) {
        List<String> logs = recentLogs == null ? List.of() : List.copyOf(recentLogs);
        String markdown = generator.generate(prediction, decision, logs);
        IncidentReportSnapshot snapshot = new IncidentReportSnapshot(
                UUID.randomUUID(),
                prediction.serviceName(),
                prediction.type().name(),
                prediction.severity().name(),
                toJson(prediction),
                toJson(decision),
                toJson(logs),
                markdown,
                Instant.now()
        );
        return store.save(snapshot);
    }

    public Optional<StoredIncidentReport> latestForService(String serviceName) {
        return store.latestForService(serviceName);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize incident report payload", exception);
        }
    }
}
