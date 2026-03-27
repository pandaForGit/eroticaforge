package com.eroticaforge.dataanalysis.charactercard.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 触发词与反应（对齐 {@code docs/guides/高质量人物卡.md}）。
 *
 * @author EroticaForge
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CharacterCardTrigger {

    private String keyword = "";
    private String reaction = "";

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword == null ? "" : keyword;
    }

    public String getReaction() {
        return reaction;
    }

    public void setReaction(String reaction) {
        this.reaction = reaction == null ? "" : reaction;
    }
}
