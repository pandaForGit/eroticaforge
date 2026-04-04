package com.eroticaforge.application.dto.api;

import java.util.List;
import java.util.Map;

/**
 * PUT /api/stories/{storyId}/state 请求体；{@code null} 字段表示不修改该项。
 *
 * @param version           期望的当前版本号（必填，与库中不一致时 409）
 * @param currentSummary    覆盖概要，{@code null} 表示保持
 * @param characterStates   覆盖人物状态 Map，{@code null} 表示保持
 * @param importantFacts    覆盖重要事实列表，{@code null} 表示保持
 * @param worldFlags        覆盖世界标记列表，{@code null} 表示保持
 * @param lastChapterEnding 覆盖上章结尾，{@code null} 表示保持
 * @author EroticaForge
 */
public record UpdateStoryStateRequest(
        Integer version,
        String currentSummary,
        Map<String, String> characterStates,
        List<String> importantFacts,
        List<String> worldFlags,
        String lastChapterEnding) {}
