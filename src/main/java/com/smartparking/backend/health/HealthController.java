package com.smartparking.backend.health;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public Map<String, String> health() {
        Integer value = jdbc.queryForObject("SELECT 1 FROM DUAL", Integer.class);
        if (value == null || value != 1) {
            throw new IllegalStateException("Oracle health check failed");
        }
        return Map.of("status", "UP", "database", "ORACLE");
    }
}
