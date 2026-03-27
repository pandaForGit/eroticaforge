package com.eroticaforge.application.dto.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * GET/PUT /api/stories/{storyId}/state 使用的状态视图。
 *
 * @param storyId           故事 ID
 * @param version           乐观锁版本（PUT 时必须与当前一致）
 * @param currentSummary    剧情摘要
 * @param characterStates   人物状态
 * @param importantFacts    重要事实
 * @param worldFlags        世界标记
 * @param lastChapterEnding 上章结尾
 * @param updatedAt         行更新时间
 * @author EroticaForge
 */
public record StoryStateResponse(
        String storyId,
        int version,
        String currentSummary,
        Map<String, String> characterStates,
        List<String> importantFacts,
        List<String> worldFlags,
        String lastChapterEnding,
        Instant updatedAt) {}
