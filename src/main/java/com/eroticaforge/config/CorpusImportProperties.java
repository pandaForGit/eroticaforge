package com.eroticaforge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 启动时一次性将数据分析模块产出的分类 JSONL 导入参考库向量（需 {@link #isEnable()} 为 true）。
 *
 * @author EroticaForge
 */
@ConfigurationProperties(prefix = "erotica.corpus-import")
public class CorpusImportProperties {

    /**
     * 为 true 时，应用启动后执行一次 {@link com.eroticaforge.application.startup.CorpusJsonlImportApplicationRunner}。
     */
    private boolean enable = false;

    /**
     * 分类结果 JSONL 路径（UTF-8），通常指向 {@code dataAnalysisModule/out/corpus_classification.jsonl}。
     */
    private String jsonlPath = "";

    /**
     * 语料根目录，须与数据分析批处理时的 {@code input-directory} 一致。
     */
    private String corpusRoot = "";

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

    public String getCorpusRoot() {
        return corpusRoot;
    }

    public void setCorpusRoot(String corpusRoot) {
        this.corpusRoot = corpusRoot == null ? "" : corpusRoot;
    }
}
