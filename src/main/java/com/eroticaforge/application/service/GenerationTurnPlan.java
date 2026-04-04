package com.eroticaforge.application.service;

/**
 * 单轮生成：面向模板的任务说明、RAG 嵌入后缀与检索修饰。
 *
 * @param taskConstraints 注入 {@code {{taskConstraints}}}
 * @param ragEmbedSuffix  追加在嵌入查询末尾（不出现在「用户原话」展示中），引导向量检索
 * @param ragModifiers    RAG 行为覆盖
 * @author EroticaForge
 */
public record GenerationTurnPlan(
        String taskConstraints, String ragEmbedSuffix, RagRetrievalModifiers ragModifiers) {

    /** 无额外任务块、默认 RAG。 */
    public static GenerationTurnPlan neutral() {
        return new GenerationTurnPlan("", "", RagRetrievalModifiers.defaults());
    }
}
