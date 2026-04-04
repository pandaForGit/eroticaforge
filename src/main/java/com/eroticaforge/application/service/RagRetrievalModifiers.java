package com.eroticaforge.application.service;

/**
 * 单轮 RAG 检索覆盖项；字段为 {@code null} 时使用 {@code application.yml} 中的默认行为。
 *
 * @param augmentSummary 是否把故事摘要拼进嵌入查询；换线类意图常为 {@code false}，减轻「摘要绑架召回」
 * @param topKOverride   返回条数上限；换线时可缩小，降低同质化旧章节切块的影响
 * @author EroticaForge
 */
public record RagRetrievalModifiers(Boolean augmentSummary, Integer topKOverride) {

    /**
     * 全部回退到全局配置。
     */
    public static RagRetrievalModifiers defaults() {
        return new RagRetrievalModifiers(null, null);
    }
}
