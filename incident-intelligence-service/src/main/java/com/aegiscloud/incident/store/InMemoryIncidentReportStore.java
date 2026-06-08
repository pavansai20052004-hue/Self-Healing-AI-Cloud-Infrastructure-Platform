package com.aegiscloud.incident.store;

import com.aegiscloud.incident.model.IncidentReportSnapshot;
import com.aegiscloud.incident.model.StoredIncidentReport;

import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryIncidentReportStore implements IncidentReportStore {
    private final ConcurrentMap<String, Deque<StoredIncidentReport>> reportsByService = new ConcurrentHashMap<>();

    @Override
    public StoredIncidentReport save(IncidentReportSnapshot snapshot) {
        StoredIncidentReport report = new StoredIncidentReport(
                snapshot.reportId(),
                snapshot.serviceName(),
                snapshot.predictionType(),
                snapshot.severity(),
                snapshot.createdAt(),
                snapshot.markdown(),
                false
        );
        reportsByService.computeIfAbsent(report.serviceName(), ignored -> new ConcurrentLinkedDeque<>()).addFirst(report);
        return report;
    }

    @Override
    public Optional<StoredIncidentReport> latestForService(String serviceName) {
        Deque<StoredIncidentReport> reports = reportsByService.get(serviceName);
        if (reports == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(reports.peekFirst());
    }
}
