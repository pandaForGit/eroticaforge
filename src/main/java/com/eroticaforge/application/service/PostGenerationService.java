package com.eroticaforge.application.service;

import com.eroticaforge.application.dto.PostGenerationResult;
import com.eroticaforge.config.GenerationProperties;
import com.eroticaforge.domain.Chapter;
import com.eroticaforge.infrastructure.persistence.ChapterRepository;
import com.eroticaforge.infrastructure.persistence.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 生成后处理：章节持久化、StoryState 更新、可选 RAG 再索引（阶段 4.4）。
 *
 * @author EroticaForge
 */
@Service
@RequiredArgsConstructor
public class PostGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PostGenerationService.class);

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final StoryStateService storyStateService;
    private final RagIngestionService ragIngestionService;
    private final GenerationProperties generationProperties;

    /**
     * 分配章节序号、写入 {@code erotica_chapters}、递增故事章节计数、更新状态；可选将正文摄入 RAG。
     *
     * @param storyId        故事主键，不可为空
     * @param generatedText  模型完整输出正文，不可为 {@code null}
     * @param userPrompt     用户本轮输入（写入章节 metadata 摘要），可为空
     * @return 新章节 ID 与序号
     */
    @Transactional(rollbackFor = Exception.class)
    public PostGenerationResult processGeneratedContent(
            String storyId, String generatedText, String userPrompt) {
        Objects.requireNonNull(generatedText, "generatedText");
        if (!StringUtils.hasText(storyId)) {
            throw new IllegalArgumentException("storyId 不能为空");
        }

        long t0 = System.nanoTime();
        Integer seq =
                storyRepository
                        .allocateNextChapterSeq(storyId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "无法分配章节序号，故事可能不存在 storyId=" + storyId));

        String chapterId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (StringUtils.hasText(userPrompt)) {
            metadata.put("user_prompt", truncate(userPrompt, 500));
        }

        Chapter chapter =
                new Chapter(
                        chapterId,
                        storyId,
                        seq,
                        "第 " + seq + " 章",
                        generatedText,
                        metadata,
                        now);
        chapterRepository.insert(chapter);
        storyRepository.incrementTotalChapters(storyId);
        storyStateService.updateState(storyId, generatedText);

        if (generationProperties.isReindexChapterToRag()) {
            Map<String, Object> ragMeta = new LinkedHashMap<>();
            ragMeta.put("chapter_id", chapterId);
            ragIngestionService.ingestChapter(storyId, generatedText, ragMeta);
        }

        long ms = (System.nanoTime() - t0) / 1_000_000L;
        int contentChars = generatedText.length();
        log.info(
                "PostGeneration 完成 storyId={} chapterId={} seq={} contentChars={} userPromptChars={} 耗时{}ms",
                storyId,
                chapterId,
                seq,
                contentChars,
                userPrompt != null ? userPrompt.length() : 0,
                ms);
        if (contentChars == 0) {
            log.warn(
                    "PostGeneration 写入章节正文为空 storyId={} chapterId={}（流式/同步均未拿到模型输出时可对照 SSE tokenFrames 与 LangChain 日志）",
                    storyId,
                    chapterId);
        }
        return new PostGenerationResult(chapterId, seq);
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "…";
    }
}
