package com.eroticaforge.application.service;

/**
 * RAG 检索结果：成功时 {@link #failureMessage()} 为 {@code null}；失败时上下文为空串且带可读说明。
 *
 * @author EroticaForge
 */
public record RagRetrievalResult(String context, String failureMessage) {

    public static RagRetrievalResult ok(String context) {
        return new RagRetrievalResult(context != null ? context : "", null);
    }

    public static RagRetrievalResult failed(String failureMessage) {
        return new RagRetrievalResult("", failureMessage);
    }
}
