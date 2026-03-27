package com.eroticaforge.presentation.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查：进程、PostgreSQL、llama OpenAI 兼容端、Ollama（嵌入）。
 *
 * @author EroticaForge
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private static final String SQL_PING = "SELECT 1";
    private static final Duration HTTP_PING_TIMEOUT = Duration.ofSeconds(2);
    private static final int HTTP_SUCCESS_MIN = 200;
    private static final int HTTP_SUCCESS_MAX = 300;

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(HTTP_PING_TIMEOUT).build();

    private final JdbcTemplate jdbcTemplate;
    private final String llmProbeUrl;
    private final String ollamaProbeUrl;

    /**
     * @param jdbcTemplate    JDBC 模板
     * @param llmBaseUrl      {@code langchain4j.open-ai.base-url}（通常以 /v1 结尾）
     * @param ollamaBaseUrl   {@code langchain4j.ollama.base-url}
     */
    public HealthController(
            JdbcTemplate jdbcTemplate,
            @Value("${langchain4j.open-ai.base-url}") String llmBaseUrl,
            @Value("${langchain4j.ollama.base-url}") String ollamaBaseUrl) {
        this.jdbcTemplate = jdbcTemplate;
        this.llmProbeUrl = trimTrailingSlash(llmBaseUrl) + "/models";
        this.ollamaProbeUrl = trimTrailingSlash(ollamaBaseUrl) + "/api/tags";
    }

    /**
     * 聚合健康状态（不校验 GPU）。
     *
     * @return JSON：{@code status}、{@code database}、{@code llm}、{@code embedding}
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>(8);
        body.put("status", "ok");
        body.put("database", checkDatabase());
        body.put("llm", pingHttp(llmProbeUrl));
        body.put("embedding", pingHttp(ollamaProbeUrl));
        return ResponseEntity.ok(body);
    }

    private String checkDatabase() {
        try {
            jdbcTemplate.queryForObject(SQL_PING, Integer.class);
            return "up";
        } catch (DataAccessException ex) {
            return "down: " + ex.getClass().getSimpleName();
        }
    }

    private String pingHttp(String url) {
        try {
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(HTTP_PING_TIMEOUT)
                            .GET()
                            .build();
            HttpResponse<Void> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int code = response.statusCode();
            if (code >= HTTP_SUCCESS_MIN && code < HTTP_SUCCESS_MAX) {
                return "up";
            }
            return "down: HTTP " + code;
        } catch (Exception ex) {
            return "down: " + ex.getClass().getSimpleName();
        }
    }

    private static String trimTrailingSlash(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
