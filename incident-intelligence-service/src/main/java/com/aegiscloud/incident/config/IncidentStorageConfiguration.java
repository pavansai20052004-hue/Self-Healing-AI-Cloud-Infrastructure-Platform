package com.aegiscloud.incident.config;

import com.aegiscloud.incident.store.InMemoryIncidentReportStore;
import com.aegiscloud.incident.store.IncidentReportStore;
import com.aegiscloud.incident.store.JdbcIncidentReportStore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class IncidentStorageConfiguration {
    @Bean
    @ConditionalOnProperty(name = "DATABASE_URL")
    public DataSource incidentDataSource(org.springframework.core.env.Environment environment) {
        String rawUrl = environment.getProperty("DATABASE_URL");
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalStateException("DATABASE_URL is empty");
        }

        String normalizedUrl = normalizeUrl(rawUrl);
        URI uri = URI.create(normalizedUrl);
        String jdbcUrl = toJdbcUrl(normalizedUrl);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        if (uri.getUserInfo() != null && uri.getUserInfo().contains(":")) {
            String[] parts = uri.getUserInfo().split(":", 2);
            config.setUsername(decode(parts[0]));
            config.setPassword(decode(parts[1]));
        }
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000L);
        config.setInitializationFailTimeout(10_000L);
        return new HikariDataSource(config);
    }

    @Bean
    @ConditionalOnProperty(name = "DATABASE_URL")
    public JdbcTemplate incidentJdbcTemplate(DataSource incidentDataSource) {
        return new JdbcTemplate(incidentDataSource);
    }

    @Bean
    @ConditionalOnProperty(name = "DATABASE_URL")
    public IncidentReportStore jdbcIncidentReportStore(JdbcTemplate incidentJdbcTemplate) {
        return new JdbcIncidentReportStore(incidentJdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(IncidentReportStore.class)
    public IncidentReportStore inMemoryIncidentReportStore() {
        return new InMemoryIncidentReportStore();
    }

    private static String normalizeUrl(String rawUrl) {
        if (rawUrl.startsWith("jdbc:")) {
            return rawUrl.substring("jdbc:".length());
        }
        return rawUrl;
    }

    private static String toJdbcUrl(String normalizedUrl) {
        if (normalizedUrl.startsWith("jdbc:")) {
            return normalizedUrl;
        }
        return "jdbc:" + normalizedUrl;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
