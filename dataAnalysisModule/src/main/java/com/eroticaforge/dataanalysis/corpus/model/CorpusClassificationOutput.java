package com.eroticaforge.dataanalysis.corpus.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 写入 JSONL 的一行：相对路径、哈希、编码 + 模型分类字段。
 *
 * @author EroticaForge
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CorpusClassificationOutput {

    @JsonProperty("source_relative_path")
    private String sourceRelativePath;

    @JsonProperty("content_sha256")
    private String contentSha256;

    @JsonProperty("source_encoding")
    private String sourceEncoding;

    private String title;

    @JsonProperty("main_type")
    private String mainType;

    private List<String> tags;

    private String style;

    private int intensity;

    private String summary;

    @JsonProperty("recommended_for")
    private List<String> recommendedFor;

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

    public String getSourceEncoding() {
        return sourceEncoding;
    }

    public void setSourceEncoding(String sourceEncoding) {
        this.sourceEncoding = sourceEncoding;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMainType() {
        return mainType;
    }

    public void setMainType(String mainType) {
        this.mainType = mainType;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public int getIntensity() {
        return intensity;
    }

    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getRecommendedFor() {
        return recommendedFor;
    }

    public void setRecommendedFor(List<String> recommendedFor) {
        this.recommendedFor = recommendedFor;
    }

    /**
     * 由 LLM 结果与文件元数据组装输出行。
     *
     * @param relativePath   相对输入根的路径
     * @param sha256Hex      正文 SHA-256（十六进制）
     * @param encoding       检测到的编码标签
     * @param classification 模型解析结果
     * @return 输出行 DTO
     */
    public static CorpusClassificationOutput from(
            String relativePath,
            String sha256Hex,
            String encoding,
            NovelLlmClassification classification) {
        CorpusClassificationOutput o = new CorpusClassificationOutput();
        o.setSourceRelativePath(relativePath);
        o.setContentSha256(sha256Hex);
        o.setSourceEncoding(encoding);
        o.setTitle(classification.getTitle());
        o.setMainType(classification.getMainType());
        o.setTags(classification.getTags());
        o.setStyle(classification.getStyle());
        o.setIntensity(classification.getIntensity());
        o.setSummary(classification.getSummary());
        o.setRecommendedFor(classification.getRecommendedFor());
        return o;
    }
}
