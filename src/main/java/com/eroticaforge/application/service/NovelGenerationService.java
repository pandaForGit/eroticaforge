package com.eroticaforge.application.service;

import com.eroticaforge.config.ChatModelRequests;
import com.eroticaforge.domain.StoryState;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.function.Consumer;

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
    private final StoryCharacterSnapshotService storyCharacterSnapshotService;
    private final PromptComposer promptComposer;
    private final GenerationIntentAnalyzer generationIntentAnalyzer;
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
        return assembleUserPrompt(storyId, userPrompt).fullPrompt();
    }

    /**
     * 非流式生成全文，并附带 RAG 降级时的可读说明（供 API 回显）。
     *
     * @param storyId       故事主键
     * @param userPrompt    用户输入
     * @param useMultiModel           多模型链占位
     * @param maxOutputTokensOverride 非空时覆盖默认 max_tokens（与前端 {@code maxTokens} 对应）
     * @return 模型输出与可选 {@code ragWarning}
     */
    public BlockingGenerationOutcome generateBlockingWithMeta(
            String storyId,
            String userPrompt,
            boolean useMultiModel,
            Integer maxOutputTokensOverride) {
        if (useMultiModel) {
            throw new UnsupportedOperationException("多模型链尚未实现（计划阶段 6）");
        }
        AssembledUserPrompt assembled = assembleUserPrompt(storyId, userPrompt);
        log.info(
                "调用同步聊天模型 storyId={} fullPromptChars={} ragDegraded={} maxOutputTokensOverride={}",
                storyId,
                assembled.fullPrompt().length(),
                StringUtils.hasText(assembled.ragFailureMessage()),
                maxOutputTokensOverride);
        ChatRequest chatRequest =
                ChatModelRequests.userMessage(
                        chatLanguageModel, assembled.fullPrompt(), maxOutputTokensOverride);
        ChatResponse chatResponse = chatLanguageModel.chat(chatRequest);
        String text =
                chatResponse.aiMessage() != null ? chatResponse.aiMessage().text() : null;
        log.info(
                "同步模型返回 storyId={} responseChars={}",
                storyId,
                text != null ? text.length() : 0);
        return new BlockingGenerationOutcome(text, assembled.ragFailureMessage());
    }

    /**
     * @param text        模型全文输出
     * @param ragWarning  RAG 失败降级时的说明；成功为 {@code null}
     */
    public record BlockingGenerationOutcome(String text, String ragWarning) {}

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
        streamGenerate(storyId, userPrompt, useMultiModel, handler, null, null);
    }

    /**
     * 流式生成；若 RAG 失败已降级，先调用 {@code onRagFailure}（例如向前端 SSE 推送警告）。
     *
     * @param onRagFailure 可为 {@code null}；入参为面向用户的短说明
     */
    public void streamGenerate(
            String storyId,
            String userPrompt,
            boolean useMultiModel,
            StreamingChatResponseHandler handler,
            Consumer<String> onRagFailure) {
        streamGenerate(storyId, userPrompt, useMultiModel, handler, onRagFailure, null);
    }

    /**
     * 流式生成；若 RAG 失败已降级，先调用 {@code onRagFailure}（例如向前端 SSE 推送警告）。
     *
     * @param onRagFailure              可为 {@code null}；入参为面向用户的短说明
     * @param maxOutputTokensOverride   非空时覆盖默认 max_tokens（与前端 {@code maxTokens} 对应）
     */
    public void streamGenerate(
            String storyId,
            String userPrompt,
            boolean useMultiModel,
            StreamingChatResponseHandler handler,
            Consumer<String> onRagFailure,
            Integer maxOutputTokensOverride) {
        Objects.requireNonNull(handler, "handler");
        if (useMultiModel) {
            throw new UnsupportedOperationException("多模型链尚未实现（计划阶段 6）");
        }
        AssembledUserPrompt assembled = assembleUserPrompt(storyId, userPrompt);
        if (StringUtils.hasText(assembled.ragFailureMessage()) && onRagFailure != null) {
            onRagFailure.accept(assembled.ragFailureMessage());
        }
        log.info(
                "调用流式聊天模型 storyId={} fullPromptChars={} ragDegraded={} streamingModelClass={} maxOutputTokensOverride={}",
                storyId,
                assembled.fullPrompt().length(),
                StringUtils.hasText(assembled.ragFailureMessage()),
                streamingChatLanguageModel.getClass().getName(),
                maxOutputTokensOverride);
        ChatRequest chatRequest =
                ChatModelRequests.userMessage(
                        streamingChatLanguageModel,
                        assembled.fullPrompt(),
                        maxOutputTokensOverride);
        streamingChatLanguageModel.chat(chatRequest, handler);
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
        return generateBlockingWithMeta(storyId, userPrompt, useMultiModel, null).text();
    }

    private AssembledUserPrompt assembleUserPrompt(String storyId, String userPrompt) {
        if (!StringUtils.hasText(storyId)) {
            throw new IllegalArgumentException("storyId 不能为空");
        }
        if (!StringUtils.hasText(userPrompt)) {
            throw new IllegalArgumentException("userPrompt 不能为空");
        }
        StoryState state = storyStateService.getCurrentState(storyId);
        GenerationTurnPlan turn = generationIntentAnalyzer.plan(userPrompt);
        String ragQuery = userPrompt;
        if (StringUtils.hasText(turn.ragEmbedSuffix())) {
            ragQuery = userPrompt + "\n" + turn.ragEmbedSuffix();
        }
        RagRetrievalResult ragResult =
                ragRetrievalService.retrieveRelevantContextResult(
                        ragQuery, state, turn.ragModifiers());
        String ragCtx = ragResult.context() != null ? ragResult.context() : "";
        log.info(
                "生成-RAG召回结果 storyId={} totalChars={} degraded={} failureMessage={}\n{}",
                storyId,
                ragCtx.length(),
                StringUtils.hasText(ragResult.failureMessage()),
                StringUtils.hasText(ragResult.failureMessage()) ? ragResult.failureMessage() : "",
                ragContextForLog(ragCtx, 24_000));
        String loreContext = userPrompt + "\n" + state.getCurrentSummary();
        String lore = lorebookService.getTriggeredDescriptions(loreContext);
        String characterProfiles = storyCharacterSnapshotService.buildProfilesTextForPrompt(storyId);
        String full =
                promptComposer.buildFullPrompt(
                        state,
                        characterProfiles,
                        ragResult.context(),
                        lore,
                        turn.taskConstraints(),
                        userPrompt);
        log.info(
                "生成-Prompt组装 storyId={} ragAugmentSummaryOverride={} ragTopKOverride={} ragEmbedSuffixChars={} ragQueryChars={} taskConstraintsChars={} ragContextChars={} fullPromptChars={} ragDegraded={}",
                storyId,
                turn.ragModifiers().augmentSummary(),
                turn.ragModifiers().topKOverride(),
                turn.ragEmbedSuffix().length(),
                ragQuery.length(),
                turn.taskConstraints().length(),
                ragResult.context() != null ? ragResult.context().length() : 0,
                full.length(),
                StringUtils.hasText(ragResult.failureMessage()));
        log.debug("生成-RAG嵌入查询全文 storyId={} ragQuery={}", storyId, ragQuery);
        log.debug("生成-taskConstraints全文 storyId={} {}", storyId, turn.taskConstraints());
        return new AssembledUserPrompt(full, ragResult.failureMessage());
    }

    private record AssembledUserPrompt(String fullPrompt, String ragFailureMessage) {}

    /** 日志用：超长 RAG 正文截断，避免单条日志撑爆采集端。 */
    private static String ragContextForLog(String ctx, int maxChars) {
        if (ctx == null || ctx.isEmpty()) {
            return "（空）";
        }
        if (maxChars <= 0 || ctx.length() <= maxChars) {
            return ctx;
        }
        return ctx.substring(0, maxChars)
                + "\n…(truncated, totalChars="
                + ctx.length()
                + ", logMaxChars="
                + maxChars
                + ")";
    }
}
