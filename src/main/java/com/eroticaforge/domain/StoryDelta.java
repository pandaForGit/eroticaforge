package com.eroticaforge.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对 {@link StoryState} 的增量更新描述；{@code null} 字段表示「不修改该项」。
 *
 * <p>各 record 组件语义：{@code currentSummary} 覆盖概要；{@code characterStatesPatch} 与人物状态合并（同键覆盖）；
 * {@code importantFactsAppend} / {@code worldFlagsAppend} 为追加列表；{@code lastChapterEnding} 覆盖上章结尾。
 *
 * @author EroticaForge
 */
public record StoryDelta(
        String currentSummary,
        Map<String, String> characterStatesPatch,
        List<String> importantFactsAppend,
        List<String> worldFlagsAppend,
        String lastChapterEnding) {

    /**
     * 构造空增量（调用 {@link #applyOnto(StoryState, Instant)} 时等价于原样拷贝叙事字段）。
     */
    public static StoryDelta empty() {
        return new StoryDelta(null, null, null, null, null);
    }

    /**
     * 在保留 {@code base} 的 {@link StoryState#getStoryId()} 与 {@link StoryState#getVersion()} 前提下合并叙事字段。
     *
     * @param base      当前状态，不可为 {@code null}
     * @param updatedAt 写入行的逻辑时间（通常 {@link Instant#now()}）
     * @return 待交给 {@link com.eroticaforge.infrastructure.persistence.StoryStateRepository#updateIfVersionMatches(StoryState)} 的新状态视图
     */
    public StoryState applyOnto(StoryState base, Instant updatedAt) {
        String sum = currentSummary != null ? currentSummary : base.getCurrentSummary();
        Map<String, String> chars = new LinkedHashMap<>(base.getCharacterStates());
        if (characterStatesPatch != null) {
            chars.putAll(characterStatesPatch);
        }
        List<String> facts = new ArrayList<>(base.getImportantFacts());
        if (importantFactsAppend != null) {
            facts.addAll(importantFactsAppend);
        }
        List<String> flags = new ArrayList<>(base.getWorldFlags());
        if (worldFlagsAppend != null) {
            flags.addAll(worldFlagsAppend);
        }
        String ending = lastChapterEnding != null ? lastChapterEnding : base.getLastChapterEnding();
        return new StoryState(
                base.getStoryId(), base.getVersion(), updatedAt, sum, chars, facts, flags, ending);
    }
}
