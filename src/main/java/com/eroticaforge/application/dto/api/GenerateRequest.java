package com.eroticaforge.application.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POST 生成接口请求体；{@code maxTokens} 暂保留字段，当前由服务端模型配置决定。
 *
 * @param prompt        用户续写/指令
 * @param maxTokens     期望最大 token（可选，未接线）
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
