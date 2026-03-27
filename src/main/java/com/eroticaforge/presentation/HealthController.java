package com.eroticaforge.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("database", checkDatabase());
        return ResponseEntity.ok(body);
    }

    private String checkDatabase() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "up";
        } catch (Exception e) {
            return "down: " + e.getClass().getSimpleName();
        }
    }
}
