package com.eroticaforge.dataanalysis.corpus.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 清洗/业务跳过记录，写入 *_skipped.jsonl。
 *
 * @author EroticaForge
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CorpusSkippedRecord {

    @JsonProperty("source_relative_path")
    private String sourceRelativePath;

    @JsonProperty("content_sha256")
    private String contentSha256;

    /** {@link CorpusSkipReason#name()}。 */
    private String reason;

    public String getSourceRelativePath() {
        return sourceRelativePath;
    }

    public void setSourceRelativePath(String sourceRelativePath) {
        this.sourceRelativePath = sourceRelativePath;
    }

    public String getContentSha256() {
        return contentSha256;
    }

    public void setContentSha256(String contentSha256) {
        this.contentSha256 = contentSha256;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * 构建跳过记录。
     *
     * @param relativePath 相对输入根路径
     * @param sha256Hex    文件字节 SHA-256
     * @param reason       原因枚举名
     * @return 记录
     */
    public static CorpusSkippedRecord of(String relativePath, String sha256Hex, CorpusSkipReason reason) {
        CorpusSkippedRecord r = new CorpusSkippedRecord();
        r.setSourceRelativePath(relativePath);
        r.setContentSha256(sha256Hex);
        r.setReason(reason.name());
        return r;
    }

    /**
     * 自定义原因字符串（如人物卡阶段的 {@code CHARACTER_CARD_PARSE}）。
     *
     * @param relativePath 相对路径
     * @param sha256Hex    哈希
     * @param reason       原因描述
     * @return 记录
     */
    public static CorpusSkippedRecord ofCustom(String relativePath, String sha256Hex, String reason) {
        CorpusSkippedRecord r = new CorpusSkippedRecord();
        r.setSourceRelativePath(relativePath);
        r.setContentSha256(sha256Hex);
        r.setReason(reason == null ? "CUSTOM" : reason);
        return r;
    }
}
