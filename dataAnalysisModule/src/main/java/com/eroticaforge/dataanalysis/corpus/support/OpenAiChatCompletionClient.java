package com.eroticaforge.dataanalysis.corpus.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 直接请求 llama-server 的 {@code /v1/chat/completions}，避免 LangChain4j 与部分服务端返回格式不一致导致 {@code aiMessage().text()} 为空。
 *
 * <p>兼容常见字段：{@code message.content}（字符串或数组）、{@code reasoning_content}、{@code reasoning}。
 *
 * @author EroticaForge
 */
@Component
public class OpenAiChatCompletionClient {

    /**
     * {@link #completeWithSystemAndLimits} 的返回值（含 {@code finish_reason}，便于判断是否因 max_tokens 截断）。
     */
    public record ChatCompletion(String text, String finishReason) {}

    private static final Logger log = LoggerFactory.getLogger(OpenAiChatCompletionClient.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final URI completionsUri;
    private final String apiKey;
    private final String modelName;
    private final double temperature;
    private final int maxTokens;

    public OpenAiChatCompletionClient(
            ObjectMapper objectMapper,
            @Value("${langchain4j.open-ai.base-url}") String baseUrl,
            @Value("${langchain4j.open-ai.api-key}") String apiKey,
            @Value("${langchain4j.open-ai.chat-model.model-name}") String modelName,
            @Value("${langchain4j.open-ai.chat-model.temperature}") double temperature,
            @Value("${langchain4j.open-ai.chat-model.max-tokens}") int maxTokens) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.completionsUri = URI.create(base + "/chat/completions");
        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(60))
                        .build();
    }

    /**
     * 单轮 user 消息，返回 assistant 可见文本（可能为空字符串）。
     *
     * @param userContent 用户全文
     * @return 模型文本
     */
    public String complete(String userContent) throws IOException, InterruptedException {
        return completeWithSystem(null, userContent);
    }

    /**
     * system + user 各一条，便于约束输出格式（如禁止思考链）。
     *
     * @param systemContent 可为 null/空白，则与 {@link #complete(String)} 相同
     */
    public String completeWithSystem(String systemContent, String userContent)
            throws IOException, InterruptedException {
        return completeWithSystemAndLimits(systemContent, userContent, 0, null).text();
    }

    /**
     * @param maxTokensOverride &gt; 0 时覆盖全局 {@link #maxTokens}
     * @param temperatureOverride 非 null 时覆盖全局 {@link #temperature}
     */
    public ChatCompletion completeWithSystemAndLimits(
            String systemContent,
            String userContent,
            int maxTokensOverride,
            Double temperatureOverride)
            throws IOException, InterruptedException {
        return completeWithSystemAndLimits(
                systemContent,
                userContent,
                maxTokensOverride,
                temperatureOverride,
                false,
                0.0,
                0.0,
                0.0);
    }

    /**
     * 人物卡等场景可附加 OpenAI 兼容采样项（llama.cpp /v1/chat/completions 多数版本会映射为抑制重复等）。
     *
     * @param useOpenAiSamplingExtensions 为 true 时写入 {@code top_p}、{@code frequency_penalty}、{@code presence_penalty}
     */
    public ChatCompletion completeWithSystemAndLimits(
            String systemContent,
            String userContent,
            int maxTokensOverride,
            Double temperatureOverride,
            boolean useOpenAiSamplingExtensions,
            double topP,
            double frequencyPenalty,
            double presencePenalty)
            throws IOException, InterruptedException {
        int effTokens = maxTokensOverride > 0 ? maxTokensOverride : maxTokens;
        double effTemp = temperatureOverride != null ? temperatureOverride : temperature;

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", modelName);
        body.put("temperature", effTemp);
        body.put("max_tokens", effTokens);
        if (useOpenAiSamplingExtensions) {
            body.put("top_p", topP);
            body.put("frequency_penalty", frequencyPenalty);
            body.put("presence_penalty", presencePenalty);
        }
        ArrayNode messages = body.putArray("messages");
        if (systemContent != null && !systemContent.isBlank()) {
            ObjectNode system = messages.addObject();
            system.put("role", "system");
            system.put("content", systemContent.trim());
        }
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userContent);

        String json = objectMapper.writeValueAsString(body);
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(completionsUri)
                        .timeout(Duration.ofMinutes(12))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String respBody = response.body();
        if (response.statusCode() != 200) {
            throw new IOException(
                    "chat/completions HTTP "
                            + response.statusCode()
                            + " body="
                            + truncate(respBody, 1200));
        }
        return extractAssistantCompletion(respBody);
    }

    private ChatCompletion extractAssistantCompletion(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choice = root.path("choices");
        if (!choice.isArray() || choice.isEmpty()) {
            log.warn("no choices in response: {}", truncate(responseBody, 800));
            return new ChatCompletion("", "");
        }
        JsonNode first = choice.get(0);
        String finish = first.path("finish_reason").asText("");
        JsonNode message = first.path("message");
        String text = coalesceMessageText(message);
        if (text.isBlank()) {
            text = first.path("text").asText("");
        }
        if (text.isBlank()) {
            text = coalesceMessageText(first);
        }
        if (text.isBlank()) {
            log.warn(
                    "empty assistant text, finish_reason={}, bodySnippet={}",
                    finish,
                    truncate(responseBody, 800));
        }
        String trimmed = text == null ? "" : text.trim();
        return new ChatCompletion(trimmed, finish);
    }

    private static String coalesceMessageText(JsonNode message) {
        if (message == null || message.isMissingNode()) {
            return "";
        }
        String s = flattenContentNode(message.get("content"));
        if (!s.isBlank()) {
            return s;
        }
        s = flattenContentNode(message.get("reasoning_content"));
        if (!s.isBlank()) {
            return s;
        }
        s = flattenContentNode(message.get("reasoning"));
        if (!s.isBlank()) {
            return s;
        }
        return flattenContentNode(message.get("text"));
    }

    private static String flattenContentNode(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText("");
        }
        if (node.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : node) {
                if (part.hasNonNull("text")) {
                    sb.append(part.get("text").asText(""));
                } else if (part.isTextual()) {
                    sb.append(part.asText(""));
                }
            }
            return sb.toString();
        }
        return "";
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
