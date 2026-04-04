package com.eroticaforge.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 阶段 4：生成与生成后处理相关开关（章节回写 RAG、状态摘要策略等）。
 *
 * @author EroticaForge
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "erotica.generation")
public class GenerationProperties {

    /**
     * 是否在 {@link com.eroticaforge.application.service.PostGenerationService} 中把新章节正文再次摄入 RAG。
     */
    private boolean reindexChapterToRag = false;

    /**
     * 写入 {@link com.eroticaforge.domain.StoryState} 的概要最大长度（模型失败时按正文截断也用此上限）。
     */
    private int stateSummaryMaxChars = 120;

    /**
     * {@code last_chapter_ending} 最多保留的尾部字符数（启发式回退时用全文尾部）。
     */
    private int lastEndingTailChars = 400;

    /**
     * 是否调用同步 {@link dev.langchain4j.model.chat.ChatLanguageModel} 从正文提炼摘要与结尾锚点；为 false 时仅用截断规则。
     */
    private boolean useLlmForStateExcerpt = true;
}
