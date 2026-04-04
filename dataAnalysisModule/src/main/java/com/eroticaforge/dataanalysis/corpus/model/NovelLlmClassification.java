package com.eroticaforge.dataanalysis.corpus.model;

import com.eroticaforge.dataanalysis.corpus.support.FlexibleIntensityDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型返回的分类 JSON 映射（字段名与 Prompt 约定一致，允许未知字段忽略）。
 *
 * @author EroticaForge
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NovelLlmClassification {

    private String title = "";

    @JsonProperty("main_type")
    private String mainType = "";

    private List<String> tags = new ArrayList<>();

    private String style = "";

    @JsonDeserialize(using = FlexibleIntensityDeserializer.class)
    private int intensity = 3;

    private String summary = "";

    @JsonProperty("recommended_for")
    private List<String> recommendedFor = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public String getMainType() {
        return mainType;
    }

    public void setMainType(String mainType) {
        this.mainType = mainType == null ? "" : mainType;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : tags;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style == null ? "" : style;
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
        this.summary = summary == null ? "" : summary;
    }

    public List<String> getRecommendedFor() {
        return recommendedFor;
    }

    public void setRecommendedFor(List<String> recommendedFor) {
        this.recommendedFor = recommendedFor == null ? new ArrayList<>() : recommendedFor;
    }
}
