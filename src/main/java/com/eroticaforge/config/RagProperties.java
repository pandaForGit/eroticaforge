package com.eroticaforge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 切分、检索相关配置（阶段 3）。
 *
 * @author EroticaForge
 */
@ConfigurationProperties(prefix = "erotica.rag")
public class RagProperties {

    /** 单段最大字符数（与《核心 Service 实现指南》512 对齐）。 */
    private int chunkSizeChars = 512;

    /** 段间重叠字符数（与指南 50 对齐）。 */
    private int chunkOverlapChars = 50;

    /** 检索返回的最大条数（指南建议 8–12，默认 10）。 */
    private int topK = 10;

    /** 最低相似度分数 [0,1]（指南建议 ≥0.75）。 */
    private double minScore = 0.75;

    /**
     * 是否在嵌入查询时拼接 {@link com.eroticaforge.domain.StoryState#getCurrentSummary()}，以提升召回相关性。
     */
    private boolean augmentQueryWithSummary = true;

    /**
     * 送入嵌入模型的查询文本最大字符数（query ± 摘要拼接后截断）。llama-server 默认 {@code ubatch-size=512} 时，
     * 中文偏多的长 prompt 易超过单次嵌入 token 上限；默认 700 字量级对应 llama ubatch 512 较稳妥，调大前请先提高嵌入端 batch。
     */
    private int queryEmbedMaxChars = 700;

    /**
     * 在已应用 metadata 范围过滤（当前故事 ± 参考库）的前提下，多取若干条再取 top-k，缓解分数边界波动。
     */
    private int searchOverfetchMultiplier = 3;

    /**
     * 专题参考库在向量与 {@code erotica_documents} 中使用的固定 story_id（勿与普通用户故事冲突）。
     */
    private String referenceCorpusStoryId = "system-corpus-reference";

    /**
     * 是否在 RAG 检索中合并参考库切块（与当前故事的切块一起参与向量检索后过滤）。
     */
    private boolean includeReferenceCorpus = false;

    public int getChunkSizeChars() {
        return chunkSizeChars;
    }

    public void setChunkSizeChars(int chunkSizeChars) {
        this.chunkSizeChars = chunkSizeChars;
    }

    public int getChunkOverlapChars() {
        return chunkOverlapChars;
    }

    public void setChunkOverlapChars(int chunkOverlapChars) {
        this.chunkOverlapChars = chunkOverlapChars;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public double getMinScore() {
        return minScore;
    }

    public void setMinScore(double minScore) {
        this.minScore = minScore;
    }

    public boolean isAugmentQueryWithSummary() {
        return augmentQueryWithSummary;
    }

    public void setAugmentQueryWithSummary(boolean augmentQueryWithSummary) {
        this.augmentQueryWithSummary = augmentQueryWithSummary;
    }

    public int getQueryEmbedMaxChars() {
        return queryEmbedMaxChars;
    }

    public void setQueryEmbedMaxChars(int queryEmbedMaxChars) {
        this.queryEmbedMaxChars = Math.max(0, queryEmbedMaxChars);
    }

    public int getSearchOverfetchMultiplier() {
        return searchOverfetchMultiplier;
    }

    public void setSearchOverfetchMultiplier(int searchOverfetchMultiplier) {
        this.searchOverfetchMultiplier = searchOverfetchMultiplier;
    }

    public String getReferenceCorpusStoryId() {
        return referenceCorpusStoryId;
    }

    public void setReferenceCorpusStoryId(String referenceCorpusStoryId) {
        this.referenceCorpusStoryId =
                referenceCorpusStoryId == null || referenceCorpusStoryId.isBlank()
                        ? "system-corpus-reference"
                        : referenceCorpusStoryId;
    }

    public boolean isIncludeReferenceCorpus() {
        return includeReferenceCorpus;
    }

    public void setIncludeReferenceCorpus(boolean includeReferenceCorpus) {
        this.includeReferenceCorpus = includeReferenceCorpus;
    }
}
