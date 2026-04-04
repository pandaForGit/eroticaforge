package com.eroticaforge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 启动时可选：将 {@code character_cards.jsonl} 导入人物卡库。
 *
 * @author EroticaForge
 */
@ConfigurationProperties(prefix = "erotica.character-cards-import")
public class CharacterCardImportProperties {

    /**
     * 为 true 时，应用启动后执行一次人物卡 JSONL 导入。
     */
    private boolean enable = false;

    /**
     * UTF-8 JSONL 路径，通常指向 {@code dataAnalysisModule/out/character_cards.jsonl}。
     */
    private String jsonlPath = "";

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getJsonlPath() {
        return jsonlPath;
    }

    public void setJsonlPath(String jsonlPath) {
        this.jsonlPath = jsonlPath == null ? "" : jsonlPath;
    }
}
