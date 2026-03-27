package com.eroticaforge.application.service;

import com.eroticaforge.application.dto.api.UpdateStoryStateRequest;
import com.eroticaforge.config.GenerationProperties;
import com.eroticaforge.domain.OptimisticLockException;
import com.eroticaforge.domain.StoryDelta;
import com.eroticaforge.domain.StoryState;
import com.eroticaforge.infrastructure.persistence.StoryStateRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * 故事状态读取与乐观锁更新（阶段 4.3）。
 *
 * @author EroticaForge
 */
@Service
@RequiredArgsConstructor
public class StoryStateService {

    private static final Logger log = LoggerFactory.getLogger(StoryStateService.class);

    private final StoryStateRepository storyStateRepository;
    private final ChatLanguageModel chatLanguageModel;
    private final GenerationProperties generationProperties;

    /**
     * 按故事 ID 读取当前状态行。
     *
     * @param storyId 故事主键，不可为空
     * @return 领域 {@link StoryState}
     * @throws IllegalArgumentException 表中无对应 {@code story_id} 时抛出
     */
    public StoryState getCurrentState(String storyId) {
        return storyStateRepository
                .findByStoryId(storyId)
                .orElseThrow(() -> new IllegalArgumentException("故事状态不存在，storyId=" + storyId));
    }

    /**
     * 在生成结束后根据正文刷新摘要、上章结尾，并追加一条自动事实；通过乐观锁写回。
     *
     * @param storyId       故事主键，不可为空
     * @param generatedText 模型生成正文，不可为 {@code null}
     */
    public void updateState(String storyId, String generatedText) {
        Objects.requireNonNull(generatedText, "generatedText");
        StoryState current = getCurrentState(storyId);
        Excerpt excerpt = resolveExcerpt(storyId, generatedText);
        List<String> newFacts = new ArrayList<>(current.getImportantFacts());
        newFacts.add("【本轮】" + excerpt.summaryLine());

        StoryState next =
                new StoryState(
                        storyId,
                        current.getVersion(),
                        Instant.now(),
                        excerpt.summary(),
                        current.getCharacterStates(),
                        newFacts,
                        current.getWorldFlags(),
                        excerpt.lastEnding());
        storyStateRepository.updateIfVersionMatches(next);
    }

    /**
     * 将增量合并到当前版本并乐观锁写回。
     *
     * @param storyId 故事主键，不可为空
     * @param delta   增量描述，不可为 {@code null}
     */
    public void mergeDelta(String storyId, StoryDelta delta) {
        Objects.requireNonNull(delta, "delta");
        StoryState current = getCurrentState(storyId);
        StoryState next = delta.applyOnto(current, Instant.now());
        storyStateRepository.updateIfVersionMatches(next);
    }

    /**
     * REST 调试：按客户端提交的版本号做乐观锁，非 null 字段覆盖写回。
     *
     * @param storyId 故事主键
     * @param req     请求体，{@code version} 必填且须与当前库中一致
     * @return 更新后的状态
     * @throws IllegalArgumentException {@code version} 为空或状态行不存在
     * @throws OptimisticLockException  版本号不一致
     */
    public StoryState replaceStateFromRest(String storyId, UpdateStoryStateRequest req) {
        Objects.requireNonNull(req, "req");
        if (req.version() == null) {
            throw new IllegalArgumentException("version 必填");
        }
        StoryState current = getCurrentState(storyId);
        if (!req.version().equals(current.getVersion())) {
            throw new OptimisticLockException(
                    "StoryState 版本不匹配，storyId=" + storyId + " 期望 version=" + current.getVersion());
        }
        StoryState next =
                new StoryState(
                        storyId,
                        current.getVersion(),
                        Instant.now(),
                        req.currentSummary() != null
                                ? req.currentSummary()
                                : current.getCurrentSummary(),
                        req.characterStates() != null
                                ? new LinkedHashMap<>(req.characterStates())
                                : new LinkedHashMap<>(current.getCharacterStates()),
                        req.importantFacts() != null
                                ? new ArrayList<>(req.importantFacts())
                                : new ArrayList<>(current.getImportantFacts()),
                        req.worldFlags() != null
                                ? new ArrayList<>(req.worldFlags())
                                : new ArrayList<>(current.getWorldFlags()),
                        req.lastChapterEnding() != null
                                ? req.lastChapterEnding()
                                : current.getLastChapterEnding());
        return storyStateRepository.updateIfVersionMatches(next);
    }

    private Excerpt resolveExcerpt(String storyId, String generatedText) {
        if (generationProperties.isUseLlmForStateExcerpt()) {
            try {
                return excerptFromLlm(generatedText);
            } catch (Exception e) {
                log.warn("状态摘录 LLM 失败，回退截断 storyId={}", storyId, e);
            }
        }
        return excerptHeuristic(generatedText);
    }

    private Excerpt excerptFromLlm(String generatedText) {
        String prompt =
                """
                你是剧情摘录助手。根据下列新生成的小说正文，输出严格两行纯文本（不要引号、不要 markdown）：
                第1行：用不超过80字概括本节剧情。
                第2行：摘录结尾约200字以内、适合作为下轮续写锚点的文字（若正文不足200字则全文）。

                正文：
                """
                + generatedText;
        String out = chatLanguageModel.chat(prompt);
        return parseTwoLineExcerpt(out, generatedText);
    }

    private Excerpt parseTwoLineExcerpt(String modelOut, String fallbackSource) {
        String[] lines =
                Arrays.stream(modelOut.strip().split("\n"))
                        .map(String::strip)
                        .filter(StringUtils::hasText)
                        .toArray(String[]::new);
        int maxSum = generationProperties.getStateSummaryMaxChars();
        int tail = generationProperties.getLastEndingTailChars();
        String first = lines.length > 0 ? truncate(lines[0], maxSum) : "";
        String last =
                lines.length > 1
                        ? truncate(lines[lines.length - 1], tail)
                        : tailOf(fallbackSource, tail);
        if (!StringUtils.hasText(first)) {
            first = truncate(fallbackSource, maxSum);
        }
        if (!StringUtils.hasText(last)) {
            last = tailOf(fallbackSource, tail);
        }
        return new Excerpt(first, last);
    }

    private Excerpt excerptHeuristic(String generatedText) {
        int maxSum = generationProperties.getStateSummaryMaxChars();
        int tail = generationProperties.getLastEndingTailChars();
        String sum = truncate(generatedText, maxSum);
        String end = tailOf(generatedText, tail);
        return new Excerpt(sum, end);
    }

    private static String truncate(String s, int maxChars) {
        if (s == null) {
            return "";
        }
        String t = s.strip();
        if (t.length() <= maxChars) {
            return t;
        }
        return t.substring(0, maxChars) + "…";
    }

    private static String tailOf(String s, int maxChars) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        String t = s.strip();
        if (t.length() <= maxChars) {
            return t;
        }
        return t.substring(t.length() - maxChars);
    }

    private record Excerpt(String summary, String lastEnding) {
        String summaryLine() {
            return summary.replace('\n', ' ').strip();
        }
    }
}
