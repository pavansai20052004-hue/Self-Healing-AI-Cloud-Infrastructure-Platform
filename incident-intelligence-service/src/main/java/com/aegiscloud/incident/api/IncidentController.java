package com.aegiscloud.incident.api;

import com.aegiscloud.core.domain.HealingDecision;
import com.aegiscloud.core.domain.Prediction;
import com.aegiscloud.core.incident.IncidentReportGenerator;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
@RequestMapping("/api/v1/incidents")
public class IncidentController {
    private final IncidentReportGenerator reportGenerator = new IncidentReportGenerator();

    @GetMapping("/healthz")
    public Map<String, String> health() {
        return Map.of("service", "incident-intelligence-service", "status", "ready");
    }

    @PostMapping("/reports")
    public ReportResponse report(@Valid @RequestBody IncidentReportRequest request) {
        String markdown = reportGenerator.generate(request.prediction(), request.decision(), request.recentLogs());
        return new ReportResponse(request.prediction().serviceName(), markdown);
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

    public record ReportResponse(
            String serviceName,
            String markdown
    ) {
    }
}
