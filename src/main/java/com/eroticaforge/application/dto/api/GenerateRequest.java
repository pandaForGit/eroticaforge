package com.eroticaforge.application.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POST 生成接口请求体；{@code maxTokens} 非空且为正时覆盖服务端默认 {@code max-tokens}（见 {@code langchain4j.open-ai.chat-model}）。
 *
 * @param prompt        用户续写/指令
 * @param maxTokens     本轮最大输出 token（可选）
 * @param useMultiModel 是否启用多模型链（阶段 6 前必须为 false）
 * @author EroticaForge
 */
public record GenerateRequest(
        String prompt,
        Integer maxTokens,
        @JsonProperty("useMultiModel") Boolean useMultiModel) {

    /**
     * @return 多模型开关，JSON 缺省时按 false
     */
    public boolean useMultiModelOrFalse() {
        return Boolean.TRUE.equals(useMultiModel);
    }
}
