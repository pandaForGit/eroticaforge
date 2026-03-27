package com.eroticaforge.application.service;

import com.eroticaforge.config.RagProperties;
import com.eroticaforge.domain.StoryState;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    @Value("${langchain4j.ollama.embedding.options.model:unknown}")
    private String ollamaEmbeddingModel;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final RagProperties ragProperties;

    /**
     * 按当前故事状态限定范围，检索与查询最相近的文本块并拼接为 Prompt 可用上下文。
     *
     * @param query 用户查询或当前续写提示
     * @param state 当前故事状态（用于 {@code story_id} 过滤；可选拼接摘要参与嵌入）
     * @return 格式化后的 RAG 文本；无命中时返回空串
     */
    public String retrieveRelevantContext(String query, StoryState state) {
        if (state == null) {
            throw new IllegalArgumentException("state 不能为 null");
        }
        if (!StringUtils.hasText(query)) {
            return "";
        }

        String storyId = state.getStoryId();
        if (!StringUtils.hasText(storyId)) {
            throw new IllegalArgumentException("state.storyId 不能为空");
        }

        try {
            String textForEmbed = buildEmbedInput(query, state);
            log.debug(
                    "RAG 嵌入请求 storyId={} model={} embedChars={} augmentSummary={}",
                    storyId,
                    ollamaEmbeddingModel,
                    textForEmbed.length(),
                    ragProperties.isAugmentQueryWithSummary());
            var queryEmbedding = embeddingModel.embed(textForEmbed).content();

            int maxFetch = Math.max(1, ragProperties.getTopK() * ragProperties.getSearchOverfetchMultiplier());
            EmbeddingSearchRequest request =
                    EmbeddingSearchRequest.builder()
                            .queryEmbedding(queryEmbedding)
                            .maxResults(maxFetch)
                            .minScore(ragProperties.getMinScore())
                            .build();

            // langchain4j 1.0.0-beta2：matches() 为原始类型 List<EmbeddingMatch>，与泛型 List 不兼容
            List<EmbeddingMatch<TextSegment>> rawMatches = embeddingStore.search(request).matches();
            List<EmbeddingMatch> scoped = new ArrayList<>();
            String refStoryId = ragProperties.getReferenceCorpusStoryId();
            boolean mergeRef = ragProperties.isIncludeReferenceCorpus();
            for (EmbeddingMatch match : rawMatches) {
                if (!(match.embedded() instanceof TextSegment seg)) {
                    continue;
                }
                String sid = seg.metadata().getString(RagMetadataKeys.STORY_ID);
                if (storyId.equals(sid)) {
                    scoped.add(match);
                    continue;
                }
                if (mergeRef
                        && refStoryId.equals(sid)
                        && RagMetadataKeys.SOURCE_REFERENCE.equalsIgnoreCase(
                                seg.metadata().getString(RagMetadataKeys.SOURCE))) {
                    scoped.add(match);
                }
            }

            // score() 返回 Double，不可用 comparingDouble(EmbeddingMatch::score)
            scoped.sort(
                    Comparator.comparing(
                            EmbeddingMatch::score, Comparator.nullsLast(Comparator.reverseOrder())));
            List<EmbeddingMatch> top = scoped.stream().limit(ragProperties.getTopK()).toList();

            if (top.isEmpty()) {
                log.debug("RAG 无命中 storyId={} queryChars={}", storyId, query.length());
                return "";
            }

            String formatted = formatMatches(top);
            log.debug("RAG 命中 storyId={} 条数={}", storyId, top.size());
            return formatted;
        } catch (Exception e) {
            // Ollama 等嵌入服务若返回 NaN，Go 端会报 json: unsupported value: NaN；此处降级为无 RAG，保证生成仍可进行
            log.warn(
                    "RAG 嵌入或检索失败，已降级为空上下文 storyId={} model={} queryChars={} cause={}",
                    storyId,
                    ollamaEmbeddingModel,
                    query.length(),
                    e.toString());
            log.debug(
                    "RAG 失败堆栈（常见根因：Ollama 嵌入输出 NaN → Go 无法 JSON 编码；可换模型/升级 Ollama/检查 GPU）",
                    e);
            return "";
        }
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
     * 将命中列表格式化为 {@code [回忆n]} 前缀的多段文本。
     *
     * @param matches 已按分数排序并截断 top-k 的列表
     * @return 拼接字符串
     */
    private static String formatMatches(List<EmbeddingMatch> matches) {
        StringBuilder sb = new StringBuilder();
        int label = 1;
        for (EmbeddingMatch match : matches) {
            if (!(match.embedded() instanceof TextSegment seg)) {
                continue;
            }
            String tag = resolveTag(seg);
            sb.append("[").append(tag).append(label).append("]\n");
            sb.append(seg.text().trim()).append("\n\n");
            label++;
        }
        return sb.toString().trim();
    }

    /**
     * 根据元数据 {@code source} 等选择标签前缀（便于后续扩展「人物卡」等类型）。
     *
     * @param segment 文本段
     * @return 标签名（不含序号），如 {@code 回忆}、{@code 人物卡}
     */
    private static String resolveTag(TextSegment segment) {
        String source = segment.metadata().getString(RagMetadataKeys.SOURCE);
        if (RagMetadataKeys.SOURCE_REFERENCE.equalsIgnoreCase(source)) {
            return RagMetadataKeys.CONTEXT_TAG_REFERENCE;
        }
        if (RagMetadataKeys.SOURCE_UPLOAD.equalsIgnoreCase(source)) {
            return RagMetadataKeys.CONTEXT_TAG_RECALL;
        }
        if (RagMetadataKeys.SOURCE_TEXT.equalsIgnoreCase(source)) {
            return RagMetadataKeys.CONTEXT_TAG_RECALL;
        }
        return RagMetadataKeys.CONTEXT_TAG_RECALL;
    }
}
