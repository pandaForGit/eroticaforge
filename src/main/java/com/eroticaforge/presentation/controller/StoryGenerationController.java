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

    /**
     * @param storyAccessService      故事存在性
     * @param novelGenerationService  构建 Prompt 与调用模型
     * @param postGenerationService   落库章节与状态
     * @param objectMapper            SSE JSON 序列化
     */
    public StoryGenerationController(
            StoryAccessService storyAccessService,
            NovelGenerationService novelGenerationService,
            PostGenerationService postGenerationService,
            ObjectMapper objectMapper) {
        this.storyAccessService = storyAccessService;
        this.novelGenerationService = novelGenerationService;
        this.postGenerationService = postGenerationService;
        this.objectMapper = objectMapper;
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

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        StringBuilder accumulated = new StringBuilder();
        try {
            novelGenerationService.streamGenerate(
                    storyId,
                    prompt,
                    false,
                    new GenerationStreamHandler(storyId, prompt, emitter, accumulated));
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
        String text = novelGenerationService.generateBlocking(storyId, prompt, false);
        var post = postGenerationService.processGeneratedContent(storyId, text, prompt);
        return ApiResponse.ok(new GenerateSyncResponse(text, post.chapterId()));
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
     * 以 {@code data:} 行推送紧凑 JSON。
     *
     * <p>勿使用 {@code data(json, APPLICATION_JSON)}：Spring 会把 {@link String} 再序列化成带引号的 JSON
     * 字符串，前端 {@code JSON.parse} 得到 string 而非对象，无法识别 {@code done} / {@code token} 帧。
     */
    private void sendJson(SseEmitter emitter, Map<String, Object> map) throws IOException {
        String json = objectMapper.writeValueAsString(map);
        emitter.send(SseEmitter.event().data(json));
    }

    /**
     * 将 LangChain4j 流式回调桥接到 SSE。
     */
    private final class GenerationStreamHandler implements StreamingChatResponseHandler {

        private final String storyId;
        private final String prompt;
        private final SseEmitter emitter;
        private final StringBuilder accumulated;

        private GenerationStreamHandler(
                String storyId,
                String prompt,
                SseEmitter emitter,
                StringBuilder accumulated) {
            this.storyId = storyId;
            this.prompt = prompt;
            this.emitter = emitter;
            this.accumulated = accumulated;
        }

        @Override
        public void onPartialResponse(String partialResponse) {
            accumulated.append(partialResponse != null ? partialResponse : "");
            try {
                Map<String, Object> tokenEvent = new LinkedHashMap<>();
                tokenEvent.put("content", partialResponse != null ? partialResponse : "");
                tokenEvent.put("type", "token");
                sendJson(emitter, tokenEvent);
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }

        @Override
        public void onCompleteResponse(ChatResponse completeResponse) {
            try {
                String text = textFrom(completeResponse, accumulated);
                try {
                    emitter.send(SseEmitter.event().comment("persisting"));
                } catch (IOException e) {
                    log.debug("SSE comment 未送出（连接可能已断）storyId={}", storyId, e);
                }
                var post = postGenerationService.processGeneratedContent(storyId, text, prompt);
                Map<String, Object> doneEvent = new LinkedHashMap<>();
                doneEvent.put("type", "done");
                doneEvent.put("done", true);
                doneEvent.put("chapterId", post.chapterId());
                sendJson(emitter, doneEvent);
                log.debug(
                        "SSE 已推送 done storyId={} chapterId={} textChars={}",
                        storyId,
                        post.chapterId(),
                        text.length());
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE 生成后处理失败 storyId={}", storyId, e);
                try {
                    Map<String, Object> err = new LinkedHashMap<>();
                    err.put(
                            "error",
                            e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                    sendJson(emitter, err);
                } catch (IOException ignored) {
                    // 连接可能已断开
                }
                emitter.completeWithError(e);
            }
        }

        @Override
        public void onError(Throwable error) {
            log.warn("SSE 模型流错误 storyId={}", storyId, error);
            try {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put(
                        "error",
                        error.getMessage() != null
                                ? error.getMessage()
                                : error.getClass().getSimpleName());
                sendJson(emitter, err);
            } catch (IOException ignored) {
                // ignore
            }
            emitter.completeWithError(error);
        }
    }
}
