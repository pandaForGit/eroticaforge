package com.eroticaforge.application.service;

import com.eroticaforge.config.PromptProperties;
import com.eroticaforge.config.RagProperties;
import com.eroticaforge.domain.StoryState;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
/**
 * 基于向量库的 RAG 检索，将命中块格式化为带标签的上下文字符串。
 *
 * @author EroticaForge
 */
@Service
@RequiredArgsConstructor
public class RagRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RagRetrievalService.class);

    @Value("${langchain4j.open-ai.embedding-model.model-name:unknown}")
    private String embeddingModelName;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final RagProperties ragProperties;
    private final PromptProperties promptProperties;

    /**
     * 按当前故事状态限定范围，检索与查询最相近的文本块并拼接为 Prompt 可用上下文。
     *
     * @param query 用户查询或当前续写提示
     * @param state 当前故事状态（用于 {@code story_id} 过滤；可选拼接摘要参与嵌入）
     * @return 格式化后的 RAG 文本；无命中时返回空串；失败时由 {@link #retrieveRelevantContextResult(String, StoryState)} 携带说明
     */
    public String retrieveRelevantContext(String query, StoryState state) {
        return retrieveRelevantContextResult(query, state).context();
    }

    /**
     * 与 {@link #retrieveRelevantContext(String, StoryState)} 相同逻辑，失败时保留 {@link RagRetrievalResult#failureMessage()} 供前端提示。
     *
     * @param query 用户查询或当前续写提示
     * @param state 当前故事状态
     * @return 上下文与可选失败说明
     */
    public RagRetrievalResult retrieveRelevantContextResult(String query, StoryState state) {
        if (state == null) {
            throw new IllegalArgumentException("state 不能为 null");
        }
        if (!StringUtils.hasText(query)) {
            return RagRetrievalResult.ok("");
        }

        String storyId = state.getStoryId();
        if (!StringUtils.hasText(storyId)) {
            throw new IllegalArgumentException("state.storyId 不能为空");
        }

        try {
            String textForEmbed = limitEmbedInputForQuery(query, state);
            log.debug(
                    "RAG 嵌入请求 storyId={} model={} embedChars={} augmentSummary={} embedMaxChars={}",
                    storyId,
                    embeddingModelName,
                    textForEmbed.length(),
                    ragProperties.isAugmentQueryWithSummary(),
                    ragProperties.getQueryEmbedMaxChars());
            var queryEmbedding = embeddingModel.embed(textForEmbed).content();

            int maxFetch = Math.max(1, ragProperties.getTopK() * ragProperties.getSearchOverfetchMultiplier());
            Filter scopeFilter = buildRagScopeMetadataFilter(storyId, ragProperties);
            EmbeddingSearchRequest request =
                    EmbeddingSearchRequest.builder()
                            .queryEmbedding(queryEmbedding)
                            .maxResults(maxFetch)
                            .minScore(ragProperties.getMinScore())
                            .filter(scopeFilter)
                            .build();

            // langchain4j 1.0.0-beta2：matches() 为原始类型 List<EmbeddingMatch>，与泛型 List 不兼容
            List<EmbeddingMatch<TextSegment>> rawMatches = embeddingStore.search(request).matches();
            List<EmbeddingMatch> scoped = new ArrayList<>(rawMatches);

            // score() 返回 Double，不可用 comparingDouble(EmbeddingMatch::score)
            scoped.sort(
                    Comparator.comparing(
                            EmbeddingMatch::score, Comparator.nullsLast(Comparator.reverseOrder())));
            List<EmbeddingMatch> top = scoped.stream().limit(ragProperties.getTopK()).toList();

            if (top.isEmpty()) {
                log.debug("RAG 无命中 storyId={} queryChars={}", storyId, query.length());
                return RagRetrievalResult.ok("");
            }

            String formatted = formatMatches(top, promptProperties);
            log.debug("RAG 命中 storyId={} 条数={}", storyId, top.size());
            return RagRetrievalResult.ok(formatted);
        } catch (Exception e) {
            // 嵌入服务若返回 NaN 等非法值，上游可能无法 JSON 编码；此处降级为无 RAG，保证生成仍可进行
            log.warn(
                    "RAG 嵌入或检索失败，已降级为空上下文 storyId={} model={} queryChars={} cause={}",
                    storyId,
                    embeddingModelName,
                    query.length(),
                    e.toString());
            log.debug(
                    "RAG 失败堆栈（常见根因：嵌入服务异常或向量非法；可检查 llama 嵌入端与模型维度）",
                    e);
            return RagRetrievalResult.failed(formatRagFailureForUser(e));
        }
    }

    private static String formatRagFailureForUser(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConnectException) {
                return "RAG 嵌入失败：无法连接嵌入服务（请确认 llama 嵌入端已启动，且 langchain4j.open-ai.embedding-model.base-url 正确）";
            }
            if (t instanceof ClosedChannelException) {
                return "RAG 嵌入失败：连接被关闭（嵌入端未监听或网络中断）";
            }
            String m = t.getMessage();
            if (StringUtils.hasText(m)
                    && (m.contains("too large")
                            || m.contains("batch size")
                            || m.contains("physical batch"))) {
                return "RAG 嵌入失败：查询文本过长，超过嵌入服务端单次可处理 token 上限（日志中常见为 llama-server 默认 ubatch 512）。"
                        + " 可在嵌入进程提高 --ubatch-size 与 --batch-size；或在本项目 erotica.rag.query-embed-max-chars 减小单次嵌入字符上限。";
            }
        }
        String msg = e.getMessage();
        if (StringUtils.hasText(msg)) {
            return "RAG 检索失败：" + msg;
        }
        return "RAG 检索失败：" + e.getClass().getSimpleName();
    }

    /**
     * 将「用户 query ± 摘要」限制在 {@link RagProperties#getQueryEmbedMaxChars()} 内，优先保留完整用户 query，再截断摘要侧。
     */
    private String limitEmbedInputForQuery(String query, StoryState state) {
        String combined = buildEmbedInput(query, state);
        int max = ragProperties.getQueryEmbedMaxChars();
        if (max <= 0 || combined.length() <= max) {
            return combined;
        }
        if (!ragProperties.isAugmentQueryWithSummary()) {
            log.debug("RAG 嵌入输入截断：原 {} 字 → {}", combined.length(), max);
            return combined.substring(0, max);
        }
        String summary = state.getCurrentSummary();
        if (!StringUtils.hasText(summary)) {
            log.debug("RAG 嵌入输入截断：原 {} 字 → {}", combined.length(), max);
            return combined.substring(0, max);
        }
        final String sep = "\n\n";
        if (query.length() >= max) {
            log.debug("RAG 嵌入输入截断：仅保留 query 前 {} 字（原 query {} 字）", max, query.length());
            return query.substring(0, max);
        }
        int budgetSummary = max - query.length() - sep.length();
        if (budgetSummary < 32) {
            log.debug("RAG 嵌入输入截断：仅保留 query {} 字（总上限 {}）", Math.min(query.length(), max), max);
            return query.substring(0, Math.min(query.length(), max));
        }
        String sumPart = summary.length() <= budgetSummary ? summary : summary.substring(0, budgetSummary);
        log.debug(
                "RAG 嵌入输入超长，截断摘要：总上限={} queryChars={} summaryUsedChars={}",
                max,
                query.length(),
                sumPart.length());
        return query + sep + sumPart;
    }

    /**
     * 构造用于嵌入的字符串（可选拼接故事摘要）。
     *
     * @param query 原始查询
     * @param state 故事状态
     * @return 送入 {@link EmbeddingModel#embed(String)} 的文本
     */
    private String buildEmbedInput(String query, StoryState state) {
        if (!ragProperties.isAugmentQueryWithSummary()) {
            return query;
        }
        String summary = state.getCurrentSummary();
        if (!StringUtils.hasText(summary)) {
            return query;
        }
        return query + "\n\n" + summary;
    }

    /**
     * 构造向量检索用的 metadata 过滤条件，由 {@link dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore}
     * 在 SQL 层应用：仅当前 {@code story_id} 的切块；若开启参考库，再并上 {@code reference story_id} 且 {@code source=reference} 的切块。
     *
     * @param currentStoryId 当前创作故事 ID
     * @param ragProperties  RAG 配置（参考库 story_id、是否合并参考库）
     * @return 非 null 的 {@link Filter}
     */
    private static Filter buildRagScopeMetadataFilter(String currentStoryId, RagProperties ragProperties) {
        Filter ownStory = new IsEqualTo(RagMetadataKeys.STORY_ID, currentStoryId);
        if (!ragProperties.isIncludeReferenceCorpus()) {
            return ownStory;
        }
        String refId = ragProperties.getReferenceCorpusStoryId();
        if (!StringUtils.hasText(refId)) {
            return ownStory;
        }
        Filter refBranch =
                Filter.and(
                        new IsEqualTo(RagMetadataKeys.STORY_ID, refId),
                        new IsEqualTo(RagMetadataKeys.SOURCE, RagMetadataKeys.SOURCE_REFERENCE));
        return Filter.or(ownStory, refBranch);
    }

    /**
     * 将命中列表格式化为 {@code [回忆n]} 前缀的多段文本。
     *
     * @param matches 已按分数排序并截断 top-k 的列表
     * @return 拼接字符串
     */
    private static String formatMatches(List<EmbeddingMatch> matches, PromptProperties promptProperties) {
        StringBuilder sb = new StringBuilder();
        int label = 1;
        for (EmbeddingMatch match : matches) {
            if (!(match.embedded() instanceof TextSegment seg)) {
                continue;
            }
            String tag = resolveTag(seg, promptProperties);
            sb.append("[").append(tag).append(label).append("]\n");
            sb.append(seg.text().trim()).append("\n\n");
            label++;
        }
        return sb.toString().trim();
    }

    /**
     * 根据元数据 {@code source} 等选择标签前缀；文案来自 {@code erotica.prompt.rag.*}。
     *
     * @param segment 文本段
     * @return 标签名（不含序号）
     */
    private static String resolveTag(TextSegment segment, PromptProperties promptProperties) {
        PromptProperties.RagLabels rag = promptProperties.getRag();
        String recall =
                StringUtils.hasText(rag.getRecallChunk())
                        ? rag.getRecallChunk()
                        : RagMetadataKeys.CONTEXT_TAG_RECALL;
        String reference =
                StringUtils.hasText(rag.getReferenceChunk())
                        ? rag.getReferenceChunk()
                        : RagMetadataKeys.CONTEXT_TAG_REFERENCE;
        String source = segment.metadata().getString(RagMetadataKeys.SOURCE);
        if (RagMetadataKeys.SOURCE_REFERENCE.equalsIgnoreCase(source)) {
            return reference;
        }
        if (RagMetadataKeys.SOURCE_UPLOAD.equalsIgnoreCase(source)) {
            return recall;
        }
        if (RagMetadataKeys.SOURCE_TEXT.equalsIgnoreCase(source)) {
            return recall;
        }
        return recall;
    }
}
