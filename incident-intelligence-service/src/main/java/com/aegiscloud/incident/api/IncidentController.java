package com.aegiscloud.incident.api;

import com.aegiscloud.core.domain.HealingDecision;
import com.aegiscloud.core.domain.Prediction;
import com.aegiscloud.incident.model.StoredIncidentReport;
import com.aegiscloud.incident.service.IncidentReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
@RequestMapping("/api/v1/incidents")
public class IncidentController {
    private final IncidentReportService incidentReportService;

    public IncidentController(IncidentReportService incidentReportService) {
        this.incidentReportService = incidentReportService;
    }

    @GetMapping("/healthz")
    public Map<String, String> health() {
        return Map.of("service", "incident-intelligence-service", "status", "ready");
    }

    @PostMapping("/reports")
    public StoredIncidentReport report(@Valid @RequestBody IncidentReportRequest request) {
        return incidentReportService.archive(request.prediction(), request.decision(), request.recentLogs());
    }

    @GetMapping("/reports/latest")
    public StoredIncidentReport latest(@RequestParam String serviceName) {
        return incidentReportService.latestForService(serviceName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No incident reports stored for " + serviceName
                ));
    }

    public record IncidentReportRequest(
            Prediction prediction,
            HealingDecision decision,
            List<String> recentLogs
    ) {
        public IncidentReportRequest {
            recentLogs = recentLogs == null ? List.of() : List.copyOf(recentLogs);
        }
    }
}
