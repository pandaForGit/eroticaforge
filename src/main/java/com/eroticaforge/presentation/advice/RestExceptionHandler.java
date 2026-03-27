package com.eroticaforge.presentation.advice;

import com.eroticaforge.application.dto.api.ApiErrorResponse;
import com.eroticaforge.domain.ChapterNotFoundException;
import com.eroticaforge.domain.OptimisticLockException;
import com.eroticaforge.domain.StoryNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.exception.InternalServerException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 统一 REST 错误体（阶段 5，对齐《API 接口定义》第 4 节）。
 *
 * @author EroticaForge
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    /** 异步 / SSE 场景下方法参数无法注入 {@link ObjectMapper}，必须字段注入。 */
    private final ObjectMapper objectMapper;

    /**
     * 故事主键不存在。
     *
     * @param ex 异常
     * @return HTTP 404
     */
    @ExceptionHandler(StoryNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> storyNotFound(StoryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(404, "not found", ex.getMessage()));
    }

    /**
     * 章节不存在。
     *
     * @param ex 异常
     * @return HTTP 404
     */
    @ExceptionHandler(ChapterNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> chapterNotFound(ChapterNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(404, "not found", ex.getMessage()));
    }

    /**
     * StoryState 乐观锁冲突。
     *
     * @param ex 异常
     * @return HTTP 409
     */
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ApiErrorResponse> optimisticLock(OptimisticLockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(409, "conflict", ex.getMessage()));
    }

    /**
     * 非法参数、业务前置校验失败。
     *
     * @param ex 异常
     * @return HTTP 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(400, "参数错误", ex.getMessage()));
    }

    /**
     * 尚未实现的能力（如多模型链）。
     *
     * @param ex 异常
     * @return HTTP 501
     */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ApiErrorResponse> notImplemented(UnsupportedOperationException ex) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(new ApiErrorResponse(501, "not implemented", ex.getMessage()));
    }

    /**
     * JSON 解析失败（如 documents 的 metadata 字段）。
     *
     * @param ex 异常
     * @return HTTP 400
     */
    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<ApiErrorResponse> jsonBad(JsonProcessingException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(400, "JSON 解析失败", ex.getMessage()));
    }

    /**
     * 超过 multipart 大小限制。
     *
     * @param ex 异常
     * @return HTTP 413
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> tooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiErrorResponse(413, "文件过大", ex.getMessage()));
    }

    /**
     * LangChain4j 调用 Ollama/OpenAI 等上游返回 5xx 或不可解析体（如嵌入含 NaN 导致 Ollama JSON 编码失败）。
     *
     * @param ex 上游异常
     * @return HTTP 502
     */
    @ExceptionHandler(InternalServerException.class)
    public Object langchainUpstream(
            InternalServerException ex, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        log.error("模型/嵌入上游错误", ex);
        ApiErrorResponse body =
                new ApiErrorResponse(
                        502,
                        "upstream error",
                        summarizeMessage(ex.getMessage()));
        return writeErrorResponse(request, response, HttpStatus.BAD_GATEWAY, body);
    }

    /**
     * LangChain4j HTTP 客户端错误（如 llama.cpp OpenAI 兼容服务返回 500：上下文超限）。
     */
    @ExceptionHandler(HttpException.class)
    public Object langchainHttp(HttpException ex, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String detail = summarizeMessage(ex.getMessage());
        boolean contextExceeded =
                detail != null
                        && (detail.contains("Context size has been exceeded")
                                || detail.contains("context size")
                                || detail.contains("context length"));
        log.warn("LangChain4j HTTP 错误 contextExceeded={} detail={}", contextExceeded, detail);
        ApiErrorResponse body =
                contextExceeded
                        ? new ApiErrorResponse(
                                400,
                                "上下文过长",
                                "模型上下文已满，请缩短 prompt、关闭 RAG 增强摘要，或增大 llama-server 的 context size。"
                                        + " 原始信息："
                                        + detail)
                        : new ApiErrorResponse(502, "LLM 请求失败", detail);
        HttpStatus status = contextExceeded ? HttpStatus.BAD_REQUEST : HttpStatus.BAD_GATEWAY;
        return writeErrorResponse(request, response, status, body);
    }

    /**
     * 兜底：记录日志并返回 500（不返回堆栈细节）。
     * 对 {@code Accept: text/event-stream} 的 SSE 请求直接写入 SSE 形态错误，避免 406 No acceptable representation。
     *
     * @param ex 异常
     * @return JSON 时为 {@link ResponseEntity}，SSE 时为 {@code null}（已写响应体）
     */
    @ExceptionHandler(Exception.class)
    public Object fallback(Exception ex, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        log.error("未处理异常", ex);
        ApiErrorResponse body =
                new ApiErrorResponse(
                        500,
                        "internal error",
                        ex.getClass().getSimpleName() + ": " + summarizeMessage(ex.getMessage()));
        return writeErrorResponse(request, response, HttpStatus.INTERNAL_SERVER_ERROR, body);
    }

    private Object writeErrorResponse(
            HttpServletRequest request, HttpServletResponse response, HttpStatus status, ApiErrorResponse body)
            throws IOException {
        if (response.isCommitted()) {
            log.warn(
                    "响应已提交（常见于 SSE 已写出首包后流失败），跳过统一错误体 status={} body.message={}",
                    status.value(),
                    body.message());
            return null;
        }
        if (acceptsEventStreamOnly(request)) {
            response.setStatus(status.value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            String json = objectMapper.writeValueAsString(body);
            String payload =
                    "event: error\ndata: " + json.replace("\n", "").replace("\r", "") + "\n\n";
            response.getOutputStream().write(payload.getBytes(StandardCharsets.UTF_8));
            response.flushBuffer();
            return null;
        }
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    /**
     * 客户端只接受 text/event-stream 时，不能用 application/json 的 {@link ResponseEntity} 做错误体。
     */
    private static boolean acceptsEventStreamOnly(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        if (accept == null || accept.isBlank()) {
            return false;
        }
        if (!accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE)) {
            return false;
        }
        return !accept.contains(MediaType.APPLICATION_JSON_VALUE)
                && !accept.contains(MediaType.ALL_VALUE);
    }

    private static String summarizeMessage(String raw) {
        if (raw == null) {
            return "";
        }
        int max = 500;
        return raw.length() <= max ? raw : raw.substring(0, max) + "…";
    }
}
