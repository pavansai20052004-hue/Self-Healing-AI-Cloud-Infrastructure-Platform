package com.aegiscloud.incident.store;

import com.aegiscloud.incident.model.IncidentReportSnapshot;
import com.aegiscloud.incident.model.StoredIncidentReport;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class JdbcIncidentReportStore implements IncidentReportStore, InitializingBean {
    private static final String SCHEMA_SQL = """
            CREATE TABLE IF NOT EXISTS incident_reports (
                report_id UUID PRIMARY KEY,
                service_name TEXT NOT NULL,
                prediction_type TEXT NOT NULL,
                severity TEXT NOT NULL,
                prediction_json TEXT NOT NULL,
                decision_json TEXT NOT NULL,
                recent_logs_json TEXT NOT NULL,
                report_markdown TEXT NOT NULL,
                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
            );

            CREATE INDEX IF NOT EXISTS idx_incident_reports_service_created_at
                ON incident_reports (service_name, created_at DESC);
            """;

    private static final String INSERT_SQL = """
            INSERT INTO incident_reports (
                report_id,
                service_name,
                prediction_type,
                severity,
                prediction_json,
                decision_json,
                recent_logs_json,
                report_markdown,
                created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String LATEST_SQL = """
            SELECT report_id, service_name, prediction_type, severity, report_markdown, created_at
            FROM incident_reports
            WHERE service_name = ?
            ORDER BY created_at DESC
            LIMIT 1
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcIncidentReportStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        jdbcTemplate.execute(SCHEMA_SQL);
    }

    @Override
    public StoredIncidentReport save(IncidentReportSnapshot snapshot) {
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(INSERT_SQL);
            statement.setObject(1, snapshot.reportId());
            statement.setString(2, snapshot.serviceName());
            statement.setString(3, snapshot.predictionType());
            statement.setString(4, snapshot.severity());
            statement.setString(5, snapshot.predictionJson());
            statement.setString(6, snapshot.decisionJson());
            statement.setString(7, snapshot.recentLogsJson());
            statement.setString(8, snapshot.markdown());
            statement.setTimestamp(9, Timestamp.from(snapshot.createdAt()));
            return statement;
        });

        return new StoredIncidentReport(
                snapshot.reportId(),
                snapshot.serviceName(),
                snapshot.predictionType(),
                snapshot.severity(),
                snapshot.createdAt(),
                snapshot.markdown(),
                true
        );
    }

    @Override
    public Optional<StoredIncidentReport> latestForService(String serviceName) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(LATEST_SQL, (rs, rowNum) -> new StoredIncidentReport(
                    UUID.fromString(rs.getString("report_id")),
                    rs.getString("service_name"),
                    rs.getString("prediction_type"),
                    rs.getString("severity"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getString("report_markdown"),
                    true
            ), serviceName));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }
}
