package com.eroticaforge.application.service;

import com.eroticaforge.domain.StoryState;
import com.eroticaforge.utils.PromptTemplates;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 单模型流式生成：RAG + {@link StoryState} + Lorebook + 用户输入（阶段 4.2）。
 *
 * @author EroticaForge
 */
@Service
@RequiredArgsConstructor
public class NovelGenerationService {

    private static final Logger log = LoggerFactory.getLogger(NovelGenerationService.class);

    private final StoryStateService storyStateService;
    private final RagRetrievalService ragRetrievalService;
    private final LorebookService lorebookService;
    private final StreamingChatLanguageModel streamingChatLanguageModel;
    private final ChatLanguageModel chatLanguageModel;

    /**
     * 组装本轮完整 user Prompt（RAG + Lorebook + StoryState），供流式/非流式共用。
     *
     * @param storyId    故事主键，不可为空
     * @param userPrompt 用户输入，不可为空
     * @return 送入模型的完整字符串
     */
    public String buildFullUserPrompt(String storyId, String userPrompt) {
        if (!StringUtils.hasText(storyId)) {
            throw new IllegalArgumentException("storyId 不能为空");
        }
        if (!StringUtils.hasText(userPrompt)) {
            throw new IllegalArgumentException("userPrompt 不能为空");
        }
        StoryState state = storyStateService.getCurrentState(storyId);
        String rag = ragRetrievalService.retrieveRelevantContext(userPrompt, state);
        String loreContext = userPrompt + "\n" + state.getCurrentSummary();
        String lore = lorebookService.getTriggeredDescriptions(loreContext);
        return PromptTemplates.buildFullPrompt(state, rag, lore, userPrompt);
    }

    /**
     * 构建完整 Prompt 并以流式方式调用主模型；生成完成后需由调用方再调 {@link PostGenerationService} 落库。
     *
     * @param storyId        故事主键，不可为空
     * @param userPrompt     用户本轮输入，不可为空
     * @param useMultiModel  为 {@code true} 时抛出（多模型链在阶段 6 实现）
     * @param handler        LangChain4j 流式回调，不可为 {@code null}
     */
    public void streamGenerate(
            String storyId,
            String userPrompt,
            boolean useMultiModel,
            StreamingChatResponseHandler handler) {
        Objects.requireNonNull(handler, "handler");
        if (useMultiModel) {
            throw new UnsupportedOperationException("多模型链尚未实现（计划阶段 6）");
        }
        String full = buildFullUserPrompt(storyId, userPrompt);
        log.debug("streamGenerate 调用流式模型 storyId={} promptChars={}", storyId, full.length());
        streamingChatLanguageModel.chat(full, handler);
    }

    /**
     * 非流式一次生成（调试用）；调用方在拿到全文后自行 {@link PostGenerationService#processGeneratedContent}。
     *
     * @param storyId        故事主键，不可为空
     * @param userPrompt     用户本轮输入，不可为空
     * @param useMultiModel  为 {@code true} 时抛出
     * @return 模型完整输出文本
     */
    public String generateBlocking(String storyId, String userPrompt, boolean useMultiModel) {
        if (useMultiModel) {
            throw new UnsupportedOperationException("多模型链尚未实现（计划阶段 6）");
        }
        String full = buildFullUserPrompt(storyId, userPrompt);
        log.debug("generateBlocking 调用同步模型 storyId={} promptChars={}", storyId, full.length());
        return chatLanguageModel.chat(full);
    }
}
