package com.eroticaforge.dataanalysis.config;

import com.eroticaforge.dataanalysis.charactercard.CharacterCardExtractorService;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * 人物卡批量提取配置（输出 JSONL，不落库）。
 *
 * @author EroticaForge
 */
@ConfigurationProperties(prefix = "erotica.character-card")
public class CharacterCardProcessingProperties {

    /** 送入模型的正文摘录最大字符数（会受 {@link #maxTotalPromptChars} 压缩）。 */
    private int maxExcerptChars = 4000;

    private int maxTotalPromptChars = 7000;

    private int minExcerptChars = 400;

    private int llmEmptyResponseRetries = 3;

    private long llmRetryDelayMs = 2000L;

    /**
     * 人物卡 completion 的 max_tokens。思考链模型往往在文末才输出 JSON，全局 4096 易截断半截。
     * ≤0 表示与全局 {@code langchain4j.open-ai.chat-model.max-tokens} 相同。
     */
    private int llmMaxOutputTokens = 12288;

    /**
     * 人物卡专用 sampling 温度（通常低于全局 0.9，减少废话、省略占位与畸形键名）。
     */
    private double llmTemperature = 0.15;

    /**
     * 是否在请求体中加入 OpenAI 兼容的 {@code top_p} / {@code frequency_penalty} / {@code presence_penalty}，
     * 用于抑制「Thinking Process」里同句循环（llama-server 多数版本支持；若接口报错可关）。
     */
    private boolean llmUseOpenAiStyleSampling = true;

    /** nucleus sampling，约 0.85～0.95；略降可减轻跑题与车轱辘话。 */
    private double llmTopP = 0.9;

    /** 重复惩罚（OpenAI 约定 -2～2）；约 0.4～0.7 常能打断模型复读。 */
    private double llmFrequencyPenalty = 0.52;

    /** 主题重复惩罚（-2～2）；略大于 0 可减少在同一话题上打转。 */
    private double llmPresencePenalty = 0.2;

    private boolean logFullPaths = false;

    /**
     * 为 true 时，对每个文件输出 INFO 级步骤日志（摘录长度、prompt 长度、模型原文长度、JSON 候选预览等）。
     */
    private boolean logTrace = false;

    /**
     * 解析失败时写入日志的模型原文最大字符数（头尾各取一半展示，中间省略）；过大时仍会在代码里封顶，避免撑爆日志。
     */
    private int logRawResponseOnFailureMaxChars = 12000;

    private boolean resumeEnabled = true;

    private boolean writeSkippedJsonl = true;

    private String outputSkippedJsonl = "";

    /** 每本小说最多提取的主要角色数量（写入 Prompt，控制输出体量）。 */
    private int maxRolesPerFile = 4;

    private final Batch batch = new Batch();

    public int getMaxExcerptChars() {
        return maxExcerptChars;
    }

    public void setMaxExcerptChars(int maxExcerptChars) {
        this.maxExcerptChars = maxExcerptChars;
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

    public int getLlmMaxOutputTokens() {
        return llmMaxOutputTokens;
    }

    public void setLlmMaxOutputTokens(int llmMaxOutputTokens) {
        this.llmMaxOutputTokens = llmMaxOutputTokens;
    }

    public double getLlmTemperature() {
        return llmTemperature;
    }

    public void setLlmTemperature(double llmTemperature) {
        this.llmTemperature = Math.max(0.0, Math.min(2.0, llmTemperature));
    }

    public boolean isLlmUseOpenAiStyleSampling() {
        return llmUseOpenAiStyleSampling;
    }

    public void setLlmUseOpenAiStyleSampling(boolean llmUseOpenAiStyleSampling) {
        this.llmUseOpenAiStyleSampling = llmUseOpenAiStyleSampling;
    }

    public double getLlmTopP() {
        return llmTopP;
    }

    public void setLlmTopP(double llmTopP) {
        this.llmTopP = Math.max(0.01, Math.min(1.0, llmTopP));
    }

    public double getLlmFrequencyPenalty() {
        return llmFrequencyPenalty;
    }

    public void setLlmFrequencyPenalty(double llmFrequencyPenalty) {
        this.llmFrequencyPenalty = Math.max(-2.0, Math.min(2.0, llmFrequencyPenalty));
    }

    public double getLlmPresencePenalty() {
        return llmPresencePenalty;
    }

    public void setLlmPresencePenalty(double llmPresencePenalty) {
        this.llmPresencePenalty = Math.max(-2.0, Math.min(2.0, llmPresencePenalty));
    }

    public boolean isLogFullPaths() {
        return logFullPaths;
    }

    public void setLogFullPaths(boolean logFullPaths) {
        this.logFullPaths = logFullPaths;
    }

    public boolean isLogTrace() {
        return logTrace;
    }

    public void setLogTrace(boolean logTrace) {
        this.logTrace = logTrace;
    }

    public int getLogRawResponseOnFailureMaxChars() {
        return logRawResponseOnFailureMaxChars;
    }

    public void setLogRawResponseOnFailureMaxChars(int logRawResponseOnFailureMaxChars) {
        this.logRawResponseOnFailureMaxChars = Math.max(0, logRawResponseOnFailureMaxChars);
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

    public int getMaxRolesPerFile() {
        return maxRolesPerFile;
    }

    public void setMaxRolesPerFile(int maxRolesPerFile) {
        this.maxRolesPerFile = Math.max(1, maxRolesPerFile);
    }

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
     * 启动时执行 {@link com.eroticaforge.dataanalysis.charactercard.CharacterCardExtractorService#extractDirectoryToJsonl}。
     *
     * @author EroticaForge
     */
    public static class Batch {

        private boolean enabled = false;
        private String inputDirectory = "";
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
