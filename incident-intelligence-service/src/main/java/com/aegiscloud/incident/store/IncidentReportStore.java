package com.aegiscloud.incident.store;

import com.aegiscloud.incident.model.IncidentReportSnapshot;
import com.aegiscloud.incident.model.StoredIncidentReport;

import java.util.Optional;

public interface IncidentReportStore {
    StoredIncidentReport save(IncidentReportSnapshot snapshot);

    Optional<StoredIncidentReport> latestForService(String serviceName);
}
