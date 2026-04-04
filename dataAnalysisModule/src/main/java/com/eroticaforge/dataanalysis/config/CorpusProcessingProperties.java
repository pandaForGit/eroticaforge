package com.eroticaforge.dataanalysis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * 语料批量清洗与分类的可选配置（不落库，输出 JSON/JSONL）。
 *
 * @author EroticaForge
 */
@ConfigurationProperties(prefix = "erotica.corpus")
public class CorpusProcessingProperties {

    /** 参与分类的最少字符数（解码、去广告后）。 */
    private int minTextChars = 300;

    /** 送入模型的正文前缀最大字符数（还会受 {@link #maxTotalPromptChars} 压缩）。 */
    private int maxPrefixChars = 1200;

    /**
     * 整段 Prompt 最大字符数（Java char 长度，近似体量上限），防止占满 llama 上下文导致**输出为空**。
     * 若超长会自动缩短「正文前缀」直到落入预算或达到 {@link #minExcerptChars}。
     */
    private int maxTotalPromptChars = 2800;

    /** 自动缩短正文前缀时的下限（再短则不再压，避免无信息量）。 */
    private int minExcerptChars = 200;

    /** 模型返回空文本时的额外重试次数（不含首次）。 */
    private int llmEmptyResponseRetries = 3;

    /** 空响应重试间隔（毫秒）。 */
    private long llmRetryDelayMs = 2000L;

    /**
     * 日志是否打印绝对路径；为 false 时仅打印相对输入根目录的路径（推荐）。
     */
    private boolean logFullPaths = false;

    /**
     * 若输出 JSONL 中已有相同「相对路径 + 内容 SHA-256」，则不再调用 LLM（断点续跑）。
     */
    private boolean resumeEnabled = true;

    /** 是否写入跳过明细文件（默认同目录 {@code *_skipped.jsonl}）。 */
    private boolean writeSkippedJsonl = true;

    /** 跳过明细路径；空则根据主输出路径推导为 {@code 主文件名_skipped.jsonl}。 */
    private String outputSkippedJsonl = "";

    /** 启动时是否自动跑一批分类（默认关闭）。 */
    private final Batch batch = new Batch();

    public int getMinTextChars() {
        return minTextChars;
    }

    public void setMinTextChars(int minTextChars) {
        this.minTextChars = minTextChars;
    }

    public int getMaxPrefixChars() {
        return maxPrefixChars;
    }

    public void setMaxPrefixChars(int maxPrefixChars) {
        this.maxPrefixChars = maxPrefixChars;
    }

    public int getMaxTotalPromptChars() {
        return maxTotalPromptChars;
    }

    public void setMaxTotalPromptChars(int maxTotalPromptChars) {
        this.maxTotalPromptChars = maxTotalPromptChars;
    }

    public int getMinExcerptChars() {
        return minExcerptChars;
    }

    public void setMinExcerptChars(int minExcerptChars) {
        this.minExcerptChars = minExcerptChars;
    }

    public int getLlmEmptyResponseRetries() {
        return llmEmptyResponseRetries;
    }

    public void setLlmEmptyResponseRetries(int llmEmptyResponseRetries) {
        this.llmEmptyResponseRetries = llmEmptyResponseRetries;
    }

    public long getLlmRetryDelayMs() {
        return llmRetryDelayMs;
    }

    public void setLlmRetryDelayMs(long llmRetryDelayMs) {
        this.llmRetryDelayMs = llmRetryDelayMs;
    }

    public boolean isLogFullPaths() {
        return logFullPaths;
    }

    public void setLogFullPaths(boolean logFullPaths) {
        this.logFullPaths = logFullPaths;
    }

    public boolean isResumeEnabled() {
        return resumeEnabled;
    }

    public void setResumeEnabled(boolean resumeEnabled) {
        this.resumeEnabled = resumeEnabled;
    }

    public boolean isWriteSkippedJsonl() {
        return writeSkippedJsonl;
    }

    public void setWriteSkippedJsonl(boolean writeSkippedJsonl) {
        this.writeSkippedJsonl = writeSkippedJsonl;
    }

    public String getOutputSkippedJsonl() {
        return outputSkippedJsonl;
    }

    public void setOutputSkippedJsonl(String outputSkippedJsonl) {
        this.outputSkippedJsonl = outputSkippedJsonl == null ? "" : outputSkippedJsonl;
    }

    /**
     * 解析跳过明细文件路径。
     *
     * @param mainOutputJsonl 主分类结果 JSONL
     * @return 路径；{@link #isWriteSkippedJsonl()} 为 false 时返回 null
     */
    public Path resolveSkippedJsonlPath(Path mainOutputJsonl) {
        if (!writeSkippedJsonl) {
            return null;
        }
        if (outputSkippedJsonl != null && !outputSkippedJsonl.isBlank()) {
            return Path.of(outputSkippedJsonl.trim());
        }
        Path abs = mainOutputJsonl.toAbsolutePath();
        Path parent = abs.getParent();
        if (parent == null) {
            parent = Path.of(".");
        }
        String name = abs.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : ".jsonl";
        return parent.resolve(base + "_skipped" + ext);
    }

    public Batch getBatch() {
        return batch;
    }

    /**
     * 可选：应用启动后自动执行
     *
     * @author EroticaForge
     */
    public static class Batch {

        private boolean enabled = false;

        /** 小说根目录（递归 .txt）。 */
        private String inputDirectory = "";

        /** 输出 JSONL 路径（追加写入）。 */
        private String outputJsonl = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getInputDirectory() {
            return inputDirectory;
        }

        public void setInputDirectory(String inputDirectory) {
            this.inputDirectory = inputDirectory == null ? "" : inputDirectory;
        }

        public String getOutputJsonl() {
            return outputJsonl;
        }

        public void setOutputJsonl(String outputJsonl) {
            this.outputJsonl = outputJsonl == null ? "" : outputJsonl;
        }
    }
}
