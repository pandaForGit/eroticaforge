package com.eroticaforge.presentation.controller;

import com.eroticaforge.application.dto.api.ApiResponse;
import com.eroticaforge.application.dto.api.GenerateRequest;
import com.eroticaforge.application.dto.api.GenerateSyncResponse;
import com.eroticaforge.application.service.NovelGenerationService;
import com.eroticaforge.application.service.PostGenerationService;
import com.eroticaforge.application.service.StoryAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * /api/stories/{storyId}/generate 与流式 SSE。
 *
 * @author EroticaForge
 */
@RestController
@RequestMapping("/api/stories/{storyId}")
public class StoryGenerationController {

    private static final Logger log = LoggerFactory.getLogger(StoryGenerationController.class);

    /** SSE 连接超时（毫秒），与长生成一致。 */
    private static final long SSE_TIMEOUT_MS = 600_000L;

    private final StoryAccessService storyAccessService;
    private final NovelGenerationService novelGenerationService;
    private final PostGenerationService postGenerationService;
    private final ObjectMapper objectMapper;
    private final String openAiBaseUrl;
    private final String openAiChatModelName;

    /**
     * @param storyAccessService      故事存在性
     * @param novelGenerationService  构建 Prompt 与调用模型
     * @param postGenerationService   落库章节与状态
     * @param objectMapper            SSE JSON 序列化
     * @param openAiBaseUrl           LangChain4j OpenAI 兼容 base-url（排障时打印）
     * @param openAiChatModelName     对话 model 名
     */
    public StoryGenerationController(
            StoryAccessService storyAccessService,
            NovelGenerationService novelGenerationService,
            PostGenerationService postGenerationService,
            ObjectMapper objectMapper,
            @Value("${langchain4j.open-ai.base-url}") String openAiBaseUrl,
            @Value("${langchain4j.open-ai.chat-model.model-name}") String openAiChatModelName) {
        this.storyAccessService = storyAccessService;
        this.novelGenerationService = novelGenerationService;
        this.postGenerationService = postGenerationService;
        this.objectMapper = objectMapper;
        this.openAiBaseUrl = openAiBaseUrl;
        this.openAiChatModelName = openAiChatModelName;
    }

    /**
     * SSE 流式生成；流结束后自动 {@link PostGenerationService#processGeneratedContent} 并推送 {@code done} 事件。
     *
     * @param storyId 故事 ID
     * @param request 用户 prompt 等
     * @return SSE 流
     */
    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @PathVariable String storyId,
            @RequestBody GenerateRequest request,
            HttpServletResponse httpResponse) {
        httpResponse.setHeader("Cache-Control", "no-store, no-transform");
        httpResponse.setHeader("X-Accel-Buffering", "no");
        storyAccessService.requireStory(storyId);
        String prompt = request != null ? request.prompt() : null;
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("prompt 不能为空");
        }
        boolean multi = request != null && request.useMultiModelOrFalse();
        if (multi) {
            throw new UnsupportedOperationException("多模型链尚未实现（计划阶段 6）");
        }

        Integer maxTok = request != null ? request.maxTokens() : null;
        log.info(
                "SSE 流式生成开始 storyId={} promptChars={} maxTokens={}",
                storyId,
                prompt.length(),
                maxTok);
        log.debug(
                "SSE 请求 prompt 摘要（前 240 字）storyId={} snippet={}",
                storyId,
                snippetForLog(prompt, 240));

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        StringBuilder accumulated = new StringBuilder();
        Consumer<String> pushRagFailure =
                msg -> {
                    try {
                        Map<String, Object> ev = new LinkedHashMap<>();
                        ev.put("type", "rag_error");
                        ev.put("error", msg);
                        sendJsonAndFlush(emitter, httpResponse, ev);
                    } catch (IOException e) {
                        log.debug("SSE RAG 警告未送出 storyId={}", storyId, e);
                    }
                };
        try {
            novelGenerationService.streamGenerate(
                    storyId,
                    prompt,
                    false,
                    new GenerationStreamHandler(
                            storyId, prompt, emitter, httpResponse, accumulated, prompt.length()),
                    pushRagFailure);
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    /**
     * 非流式一次生成并落库。
     *
     * @param storyId 故事 ID
     * @param request 请求体
     * @return 全文与 chapterId
     */
    @PostMapping("/generate")
    public ApiResponse<GenerateSyncResponse> generateSync(
            @PathVariable String storyId, @RequestBody GenerateRequest request) {
        storyAccessService.requireStory(storyId);
        String prompt = request != null ? request.prompt() : null;
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("prompt 不能为空");
        }
        boolean multi = request != null && request.useMultiModelOrFalse();
        if (multi) {
            throw new UnsupportedOperationException("多模型链尚未实现（计划阶段 6）");
        }
        Integer maxTok = request != null ? request.maxTokens() : null;
        log.info(
                "同步生成开始 storyId={} promptChars={} maxTokens={}",
                storyId,
                prompt.length(),
                maxTok);
        log.debug(
                "同步生成 prompt 摘要 storyId={} snippet={}",
                storyId,
                snippetForLog(prompt, 240));

        var outcome = novelGenerationService.generateBlockingWithMeta(storyId, prompt, false);
        log.info(
                "同步生成模型返回 storyId={} textChars={} ragWarning={}",
                storyId,
                outcome.text() != null ? outcome.text().length() : 0,
                outcome.ragWarning() != null);
        var post =
                postGenerationService.processGeneratedContent(storyId, outcome.text(), prompt);
        return ApiResponse.ok(
                new GenerateSyncResponse(outcome.text(), post.chapterId(), outcome.ragWarning()));
    }

    private static String snippetForLog(String text, int maxChars) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String t = text.replace("\r\n", "\n").replace('\r', '\n');
        if (t.length() <= maxChars) {
            return t;
        }
        return t.substring(0, maxChars) + "…";
    }

    private static String truncateForLog(String s, int maxChars) {
        if (s == null) {
            return "null";
        }
        if (s.length() <= maxChars) {
            return s;
        }
        return s.substring(0, maxChars) + "…(truncated,totalChars=" + s.length() + ")";
    }

    /**
     * tokenFrames=0 且最终正文为空时输出 LangChain4j 侧可见信息，便于对照 llama-server / vLLM 的 SSE 与 OpenAI 规范。
     */
    private void logLangChainStreamEmptyDiagnostics(
            String storyId,
            ChatResponse completeResponse,
            int userPromptChars,
            int tokenFrames,
            int nonEmptyTokenFrames,
            long aggregatePartialChars) {
        log.warn(
                "流式排障-配置 storyId={} langchain4j.open-ai.base-url={} chat-model.model-name={} userPromptChars={}",
                storyId,
                openAiBaseUrl,
                openAiChatModelName,
                userPromptChars);
        log.warn(
                "流式排障-计数 storyId={} tokenFrames={} nonEmptyTokenFrames={} aggregatePartialChars={}",
                storyId,
                tokenFrames,
                nonEmptyTokenFrames,
                aggregatePartialChars);
        if (completeResponse == null) {
            log.warn(
                    "流式排障-ChatResponse 为 null storyId={}（LangChain4j OpenAiStreamingResponseBuilder 未拼出最终响应）。"
                            + " 与 tokenFrames=0 同时出现时，高度怀疑：① 当前依赖为 langchain4j 1.0.0-beta2，其 Delta 仅解析 delta.content，"
                            + "不处理 reasoning_content；Qwen3 等「思考」模型经 llama.cpp server 流式时常把可见输出放在 reasoning_content，"
                            + "导致既不调用 onPartialResponse，build() 也可能为 null。② 或 SSE 根本未解析出含 choices[].delta 的 JSON。"
                            + " 处理建议：升级 LangChain4j 至 1.0.0+ 并为 OpenAiStreamingChatModel 开启 returnThinking（若 API 支持）；"
                            + "或在 llama-server 侧关闭 thinking / 换非思考模板，使正文进入 content；或用 curl 对 {} 发 stream 请求核对原始 SSE 字段。",
                    storyId,
                    openAiBaseUrl);
            log.warn(
                    "流式排障-建议临时打开 DEBUG：logging.level.dev.langchain4j.model.openai=DEBUG（日志量大）storyId={}",
                    storyId);
            return;
        }
        log.warn(
                "流式排障-ChatResponse.finishReason={} ChatResponse.tokenUsage={}",
                completeResponse.finishReason(),
                completeResponse.tokenUsage());
        if (completeResponse.metadata() != null) {
            var meta = completeResponse.metadata();
            log.warn(
                    "流式排障-metadata id={} modelName={} finishReason={} tokenUsage={} metadata.toString={}",
                    meta.id(),
                    meta.modelName(),
                    meta.finishReason(),
                    meta.tokenUsage(),
                    truncateForLog(meta.toString(), 2000));
        } else {
            log.warn("流式排障-metadata 为 null storyId={}", storyId);
        }
        log.warn(
                "流式排障-ChatResponse.toString={}",
                truncateForLog(completeResponse.toString(), 4000));
        if (completeResponse.aiMessage() == null) {
            log.warn("流式排障-aiMessage 为 null storyId={}", storyId);
            return;
        }
        var ai = completeResponse.aiMessage();
        String aiText = ai.text();
        log.warn(
                "流式排障-aiMessage type={} textIsNull={} textChars={} hasToolExecutionRequests={} toolRequestCount={}",
                ai.type(),
                aiText == null,
                aiText != null ? aiText.length() : -1,
                ai.hasToolExecutionRequests(),
                ai.toolExecutionRequests() != null ? ai.toolExecutionRequests().size() : -1);
        if (ai.hasToolExecutionRequests()) {
            log.warn("流式排障-aiMessage.toolExecutionRequests={}", ai.toolExecutionRequests());
        }
        if (StringUtils.hasText(aiText)) {
            log.warn(
                    "流式排障-说明：存在最终 aiMessage.text（{} 字）但未收到流式片段，可能上游只在最后一条 chunk 里带全文；前 500 字摘要={}",
                    aiText.length(),
                    snippetForLog(aiText, 500));
        }
        log.warn(
                "流式排障-常见根因：① 上游 /v1/chat/completions 未使用 stream=true 或响应体不是 text/event-stream；"
                        + "② SSE 行内 JSON 与 OpenAI 不一致（例如 delta 在 message 下、或 content 为数组）；"
                        + "③ llama.cpp server 版本与 LangChain4j 1.0.0-beta2 的解析不兼容。"
                        + " 可临时在 application.yml 设置 logging.level.dev.langchain4j.model.openai=DEBUG 查看 HTTP/SSE 细节（日志量大）。");
    }

    private static String textFrom(ChatResponse response, StringBuilder accumulated) {
        if (response != null
                && response.aiMessage() != null
                && StringUtils.hasText(response.aiMessage().text())) {
            return response.aiMessage().text();
        }
        return accumulated.toString();
    }

    /**
     * 发送一帧 SSE JSON，并尽量立刻刷出缓冲区，避免 Tomcat/代理攒包导致前端长时间看不到 token。
     *
     * <p>勿对 data 再包一层 APPLICATION_JSON：否则 String 会被二次 JSON 编码，前端解析不到 {@code done}/{@code token}。
     */
    private void sendJsonAndFlush(SseEmitter emitter, HttpServletResponse response, Map<String, Object> map)
            throws IOException {
        String json = objectMapper.writeValueAsString(map);
        emitter.send(SseEmitter.event().data(json));
        try {
            response.flushBuffer();
        } catch (IllegalStateException ex) {
            log.trace("SSE flushBuffer 未执行（异步阶段可能已提交）: {}", ex.toString());
        }
    }

    /**
     * 将 LangChain4j 流式回调桥接到 SSE。
     */
    private final class GenerationStreamHandler implements StreamingChatResponseHandler {

        private final String storyId;
        private final String prompt;
        private final SseEmitter emitter;
        private final HttpServletResponse httpResponse;
        private final StringBuilder accumulated;
        private final int userPromptChars;
        private int tokenFrameCount;
        private int nonEmptyTokenFrameCount;
        private long aggregatePartialChars;

        private GenerationStreamHandler(
                String storyId,
                String prompt,
                SseEmitter emitter,
                HttpServletResponse httpResponse,
                StringBuilder accumulated,
                int userPromptChars) {
            this.storyId = storyId;
            this.prompt = prompt;
            this.emitter = emitter;
            this.httpResponse = httpResponse;
            this.accumulated = accumulated;
            this.userPromptChars = userPromptChars;
        }

        @Override
        public void onPartialResponse(String partialResponse) {
            String chunk = partialResponse != null ? partialResponse : "";
            accumulated.append(chunk);
            tokenFrameCount++;
            if (!chunk.isEmpty()) {
                nonEmptyTokenFrameCount++;
            }
            aggregatePartialChars += chunk.length();
            if (tokenFrameCount <= 3 || tokenFrameCount % 200 == 0) {
                log.debug(
                        "SSE token 帧 storyId={} frameIndex={} chunkChars={} aggregateChars={}",
                        storyId,
                        tokenFrameCount,
                        chunk.length(),
                        aggregatePartialChars);
            }
            try {
                Map<String, Object> tokenEvent = new LinkedHashMap<>();
                tokenEvent.put("content", chunk);
                tokenEvent.put("type", "token");
                sendJsonAndFlush(emitter, httpResponse, tokenEvent);
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }

        @Override
        public void onCompleteResponse(ChatResponse completeResponse) {
            try {
                String text = textFrom(completeResponse, accumulated);
                int finalChars = text != null ? text.length() : 0;
                if (finalChars == 0) {
                    log.warn(
                            "SSE 模型结束但正文为空 storyId={} tokenFrames={} nonEmptyTokenFrames={} aggregatePartialChars={}",
                            storyId,
                            tokenFrameCount,
                            nonEmptyTokenFrameCount,
                            aggregatePartialChars);
                    logLangChainStreamEmptyDiagnostics(
                            storyId,
                            completeResponse,
                            userPromptChars,
                            tokenFrameCount,
                            nonEmptyTokenFrameCount,
                            aggregatePartialChars);
                } else {
                    log.info(
                            "SSE 模型流结束 storyId={} finalTextChars={} tokenFrames={} nonEmptyTokenFrames={}",
                            storyId,
                            finalChars,
                            tokenFrameCount,
                            nonEmptyTokenFrameCount);
                }
                try {
                    emitter.send(SseEmitter.event().comment("persisting"));
                    httpResponse.flushBuffer();
                } catch (IllegalStateException ex) {
                    log.trace("SSE comment 后 flush 跳过: {}", ex.toString());
                } catch (IOException e) {
                    log.debug("SSE comment 未送出（连接可能已断）storyId={}", storyId, e);
                }
                var post = postGenerationService.processGeneratedContent(storyId, text, prompt);
                Map<String, Object> doneEvent = new LinkedHashMap<>();
                doneEvent.put("type", "done");
                doneEvent.put("done", true);
                doneEvent.put("chapterId", post.chapterId());
                doneEvent.put("generatedChars", finalChars);
                doneEvent.put("tokenFrames", tokenFrameCount);
                doneEvent.put("nonEmptyTokenFrames", nonEmptyTokenFrameCount);
                sendJsonAndFlush(emitter, httpResponse, doneEvent);
                log.info(
                        "SSE 已推送 done storyId={} chapterId={} textChars={} tokenFrames={} nonEmptyTokenFrames={}",
                        storyId,
                        post.chapterId(),
                        finalChars,
                        tokenFrameCount,
                        nonEmptyTokenFrameCount);
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE 生成后处理失败 storyId={}", storyId, e);
                try {
                    Map<String, Object> err = new LinkedHashMap<>();
                    err.put(
                            "error",
                            e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                    sendJsonAndFlush(emitter, httpResponse, err);
                } catch (IOException ignored) {
                    // 连接可能已断开
                }
                emitter.completeWithError(e);
            }
        }

        @Override
        public void onError(Throwable error) {
            log.warn(
                    "SSE 模型流错误 storyId={} tokenFrames={} nonEmptyTokenFrames={}",
                    storyId,
                    tokenFrameCount,
                    nonEmptyTokenFrameCount,
                    error);
            try {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put(
                        "error",
                        error.getMessage() != null
                                ? error.getMessage()
                                : error.getClass().getSimpleName());
                sendJsonAndFlush(emitter, httpResponse, err);
            } catch (IOException ignored) {
                // ignore
            }
            emitter.completeWithError(error);
        }
    }
}
