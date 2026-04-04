package com.eroticaforge.dataanalysis.corpus;

import com.eroticaforge.dataanalysis.config.CorpusProcessingProperties;
import com.eroticaforge.dataanalysis.corpus.model.CorpusClassificationOutput;
import com.eroticaforge.dataanalysis.corpus.model.CorpusSanitizeOutcome;
import com.eroticaforge.dataanalysis.corpus.model.CorpusSkippedRecord;
import com.eroticaforge.dataanalysis.corpus.model.CorpusSkipReason;
import com.eroticaforge.dataanalysis.corpus.model.NovelLlmClassification;
import com.eroticaforge.dataanalysis.corpus.support.LlmJsonExtractor;
import com.eroticaforge.dataanalysis.corpus.support.OpenAiChatCompletionClient;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 小说语料：递归遍历 .txt，清洗后串行调用 LLM 分类，结果追加写入 JSONL（不落库）。
 *
 * <p>支持断点续跑（已分类的 相对路径+SHA256 跳过）与跳过明细 {@code *_skipped.jsonl}。
 *
 * @author EroticaForge
 */
@Service
public class NovelClassifierService {

    private static final Logger log = LoggerFactory.getLogger(NovelClassifierService.class);

    /**
     * 尽量短：n_ctx 较小时，Prompt 过长会导致 completion 为空。
     */
    private static final String TAG_POOL =
    """
    性癖与玩法：NTR、SM、调教、支配服从、脑洗、催眠、扶她、百合、乱伦、纯爱、多P、露出、监禁、人外、重口、轻度 等（可补充）；
    时代与背景：现代、都市、校园、职场、古代、架空、末世、异世界、游戏世界 等；
    人物性格与关系：傲娇、病娇、腹黑、温柔、强势、懦弱、青梅竹马、师生、上下级 等；
    环境与氛围：日常、悬疑、黑暗、喜剧、写实、二次元轻小说风、第一人称 等。""";

    private final OpenAiChatCompletionClient chatCompletionClient;
    private final CorpusSanitizer corpusSanitizer;
    private final CorpusProcessingProperties corpusProperties;
    private final ObjectMapper corpusObjectMapper;

    public NovelClassifierService(
            OpenAiChatCompletionClient chatCompletionClient,
            CorpusSanitizer corpusSanitizer,
            CorpusProcessingProperties corpusProperties,
            ObjectMapper objectMapper) {
        this.chatCompletionClient = chatCompletionClient;
        this.corpusSanitizer = corpusSanitizer;
        this.corpusProperties = corpusProperties;
        this.corpusObjectMapper =
                objectMapper
                        .copy()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 递归处理目录下所有 .txt：深度优先、单线程顺序执行，每成功一条追加一行 JSONL。
     *
     * @param inputRoot   小说根目录（含子文件夹）
     * @param outputJsonl 输出文件（不存在则创建；已存在则追加）
     * @return 统计信息
     */
    public BatchStats classifyDirectoryToJsonl(Path inputRoot, Path outputJsonl) throws IOException {
        if (!Files.isDirectory(inputRoot)) {
            throw new IllegalArgumentException("not a directory: " + inputRoot);
        }
        Path parent = outputJsonl.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path skippedPath = corpusProperties.resolveSkippedJsonlPath(outputJsonl);
        if (skippedPath != null && skippedPath.getParent() != null) {
            Files.createDirectories(skippedPath.getParent());
        }

        Set<String> classifiedKeys = new HashSet<>();
        Set<String> skipKeys = new HashSet<>();
        if (corpusProperties.isResumeEnabled()) {
            loadKeysFromJsonl(outputJsonl, classifiedKeys, "success");
            if (skippedPath != null && Files.exists(skippedPath)) {
                loadKeysFromJsonl(skippedPath, skipKeys, "skipped");
            }
        }

        BatchStats stats = new BatchStats();
        try (Stream<Path> paths = Files.walk(inputRoot);
                BufferedWriter outWriter =
                        Files.newBufferedWriter(
                                outputJsonl,
                                StandardCharsets.UTF_8,
                                java.nio.file.StandardOpenOption.CREATE,
                                java.nio.file.StandardOpenOption.APPEND)) {
            BufferedWriter skipWriter = openSkippedWriter(skippedPath);
            try {
                paths.filter(Files::isRegularFile)
                        .filter(NovelClassifierService::isTxt)
                        .sorted()
                        .forEachOrdered(
                                path -> {
                                    try {
                                        ProcessingOutcome o =
                                                processOneFile(
                                                        inputRoot,
                                                        path,
                                                        outWriter,
                                                        skipWriter,
                                                        classifiedKeys,
                                                        skipKeys);
                                        switch (o) {
                                            case SUCCESS -> stats.recordSuccess();
                                            case SKIPPED_SANITIZE -> stats.recordSkipped();
                                            case RESUMED_CLASSIFIED -> stats.recordResumedClassified();
                                            case RESUMED_SKIPPED -> stats.recordResumedSkipped();
                                        }
                                    } catch (Exception ex) {
                                        stats.recordFailed();
                                        log.warn(
                                                "classify failed: {} — {}",
                                                displayPath(inputRoot, path),
                                                ex.getMessage());
                                    }
                                });
            } finally {
                if (skipWriter != null) {
                    skipWriter.close();
                }
            }
        }
        log.info(
                "corpus classify done: success={}, skipped={}, failed={}, resumedClassified={}, resumedSkipped={}",
                stats.getSuccess(),
                stats.getSkipped(),
                stats.getFailed(),
                stats.getResumedClassified(),
                stats.getResumedSkipped());
        return stats;
    }

    private BufferedWriter openSkippedWriter(Path skippedPath) throws IOException {
        if (skippedPath == null) {
            return null;
        }
        return Files.newBufferedWriter(
                skippedPath,
                StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
    }

    private void loadKeysFromJsonl(Path file, Set<String> keys, String label) {
        try {
            if (!Files.exists(file)) {
                return;
            }
            try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
                lines.forEach(
                        line -> {
                            if (line.isBlank()) {
                                return;
                            }
                            try {
                                JsonNode n = corpusObjectMapper.readTree(line);
                                String rel = n.path("source_relative_path").asText("");
                                String hash = n.path("content_sha256").asText("");
                                if (!rel.isEmpty() && !hash.isEmpty()) {
                                    keys.add(rel + "\t" + hash);
                                }
                            } catch (Exception ex) {
                                log.warn("ignore bad {} jsonl line in {}: {}", label, file, ex.getMessage());
                            }
                        });
            }
        } catch (IOException ex) {
            log.warn("could not read {} file {}: {}", label, file, ex.getMessage());
        }
    }

    private ProcessingOutcome processOneFile(
            Path inputRoot,
            Path file,
            BufferedWriter outWriter,
            BufferedWriter skipWriter,
            Set<String> classifiedKeys,
            Set<String> skipKeys)
            throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        String sha256 = sha256Hex(bytes);
        String relative = inputRoot.relativize(file).toString().replace('\\', '/');
        String key = relative + "\t" + sha256;

        if (corpusProperties.isResumeEnabled() && classifiedKeys.contains(key)) {
            log.debug("resume: already classified {}", displayPath(inputRoot, file));
            return ProcessingOutcome.RESUMED_CLASSIFIED;
        }
        if (corpusProperties.isResumeEnabled() && skipKeys.contains(key)) {
            log.debug("resume: already in skipped list {}", displayPath(inputRoot, file));
            return ProcessingOutcome.RESUMED_SKIPPED;
        }

        CorpusSanitizer.Result sanitize = corpusSanitizer.sanitizeBytes(bytes);
        if (!sanitize.isOk()) {
            appendSkipIfNeeded(skipWriter, relative, sha256, sanitize.skipReason(), skipKeys, key);
            logSkip(displayPath(inputRoot, file), sanitize.skipReason());
            return ProcessingOutcome.SKIPPED_SANITIZE;
        }

        CorpusSanitizeOutcome outcome = sanitize.outcome();
        String prompt =
                buildClassificationPromptWithinBudget(
                        file.getFileName().toString(), relative, outcome.utf16Text());
        String rawReply = invokeChatWithRetries(prompt);
        NovelLlmClassification parsed;
        try {
            String json = LlmJsonExtractor.extractJsonObject(rawReply);
            parsed = corpusObjectMapper.readValue(json, NovelLlmClassification.class);
        } catch (Exception ex) {
            throw new IOException("parse classification JSON failed: " + ex.getMessage(), ex);
        }
        CorpusClassificationOutput line =
                CorpusClassificationOutput.from(relative, sha256, outcome.detectedCharset(), parsed);
        outWriter.write(corpusObjectMapper.writeValueAsString(line));
        outWriter.newLine();
        outWriter.flush();
        classifiedKeys.add(key);
        log.info("classified [{}] {}", displayPath(inputRoot, file), line.getTitle());
        return ProcessingOutcome.SUCCESS;
    }

    private void appendSkipIfNeeded(
            BufferedWriter skipWriter,
            String relative,
            String sha256,
            CorpusSkipReason reason,
            Set<String> skipKeys,
            String key)
            throws IOException {
        if (skipWriter == null) {
            return;
        }
        CorpusSkippedRecord rec = CorpusSkippedRecord.of(relative, sha256, reason);
        skipWriter.write(corpusObjectMapper.writeValueAsString(rec));
        skipWriter.newLine();
        skipWriter.flush();
        skipKeys.add(key);
    }

    private void logSkip(String displayPath, CorpusSkipReason reason) {
        log.debug("skip {} reason={}", displayPath, reason);
    }

    private String displayPath(Path inputRoot, Path file) {
        if (corpusProperties.isLogFullPaths()) {
            return file.toAbsolutePath().toString();
        }
        return inputRoot.relativize(file).toString().replace('\\', '/');
    }

    private static boolean isTxt(Path p) {
        return p.getFileName().toString().toLowerCase().endsWith(".txt");
    }

    /**
     * 在不超过 {@link CorpusProcessingProperties#getMaxTotalPromptChars()} 的前提下组装 Prompt；过长则缩短正文前缀。
     */
    private String buildClassificationPromptWithinBudget(
            String fileName, String relativePath, String fullCleanedText) {
        int minEx = corpusProperties.getMinExcerptChars();
        int maxTotal = corpusProperties.getMaxTotalPromptChars();
        int capPrefix = corpusProperties.getMaxPrefixChars();
        int excerptLen = Math.min(fullCleanedText.length(), capPrefix);
        String excerpt = fullCleanedText.substring(0, excerptLen);
        while (true) {
            String prompt = buildClassificationPrompt(fileName, relativePath, excerpt);
            if (prompt.length() <= maxTotal) {
                return prompt;
            }
            if (excerpt.length() <= minEx) {
                log.warn(
                        "prompt still {} chars (> maxTotalPromptChars {}); may cause empty LLM output on small context",
                        prompt.length(),
                        maxTotal);
                return prompt;
            }
            int next = Math.max(minEx, excerpt.length() * 3 / 4);
            excerpt = fullCleanedText.substring(0, Math.min(next, fullCleanedText.length()));
        }
    }

    private String invokeChatWithRetries(String prompt) throws IOException {
        int maxAttempts = Math.max(1, corpusProperties.getLlmEmptyResponseRetries() + 1);
        long delayMs = Math.max(0L, corpusProperties.getLlmRetryDelayMs());
        IOException lastIo = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String text = chatCompletionClient.complete(prompt);
                if (text != null && !text.isBlank()) {
                    return text;
                }
                log.warn(
                        "LLM returned empty text (attempt {}/{}), promptChars={}. "
                                + "已用直连 HTTP 解析；若仍空请查 llama-server 控制台、或 curl /v1/chat/completions 看原始 JSON。",
                        attempt,
                        maxAttempts,
                        prompt.length());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException("LLM call interrupted", ex);
            } catch (IOException ex) {
                lastIo = ex;
                log.warn("LLM call exception (attempt {}/{}): {}", attempt, maxAttempts, ex.getMessage());
            } catch (Exception ex) {
                lastIo = new IOException("LLM call failed: " + ex.getMessage(), ex);
                log.warn("LLM call exception (attempt {}/{}): {}", attempt, maxAttempts, ex.getMessage());
            }
            if (attempt < maxAttempts && delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("interrupted during LLM retry wait", ie);
                }
            }
        }
        if (lastIo != null) {
            throw lastIo;
        }
        throw new IOException(
                "LLM returned empty text after "
                        + maxAttempts
                        + " attempts; promptChars="
                        + prompt.length());
    }

    private static String buildClassificationPrompt(String fileName, String relativePath, String excerpt) {
        return """
          你是小说语料分类助手。请根据文件名、相对路径与正文前缀，输出**唯一一段**合法 JSON（不要 markdown、不要解释）。
          【标签池】请尽量从下列维度中选词，也可新增不在池中但合理的标签（中文或通用英文缩写均可）：
          %s
          文件名：%s
          相对路径：%s
          正文前缀（已截断）：
          %s
          严格使用以下 JSON 键名与类型（tags、recommended_for 为字符串数组；intensity 为 1～5 的整数）：
          {
            "title": "推断或沿用文件名中的标题",
            "main_type": "主类型/背景概括，如 现代都市 / 二次元 / 古风 等",
            "tags": ["从池子选或新增，可多选"],
            "style": "叙事与文笔风格概括",
            "intensity": 3,
            "summary": "一句话剧情摘要，30 字以内",
            "recommended_for": ["适合作为哪类创作的参考，如 NTR 长篇、校园纯爱 等"]
          }
          """
                .formatted(TAG_POOL, fileName, relativePath, excerpt);
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private enum ProcessingOutcome {
        SUCCESS,
        SKIPPED_SANITIZE,
        RESUMED_CLASSIFIED,
        RESUMED_SKIPPED
    }

    /**
     * 批量运行统计。
     *
     * @author EroticaForge
     */
    public static final class BatchStats {
        private int success;
        private int skipped;
        private int failed;
        private int resumedClassified;
        private int resumedSkipped;

        void recordSuccess() {
            success++;
        }

        void recordSkipped() {
            skipped++;
        }

        void recordFailed() {
            failed++;
        }

        void recordResumedClassified() {
            resumedClassified++;
        }

        void recordResumedSkipped() {
            resumedSkipped++;
        }

        public int getSuccess() {
            return success;
        }

        public int getSkipped() {
            return skipped;
        }

        public int getFailed() {
            return failed;
        }

        public int getResumedClassified() {
            return resumedClassified;
        }

        public int getResumedSkipped() {
            return resumedSkipped;
        }
    }
}
