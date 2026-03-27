package com.eroticaforge.application.service;

import com.eroticaforge.utils.CorpusTextFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HexFormat;
import java.util.stream.Stream;

/**
 * 将 {@code dataAnalysisModule} 产出的 {@code corpus_classification.jsonl} 与磁盘上的 txt 对齐，批量写入 RAG 参考库。
 *
 * <p>写入时使用 {@link RagProperties#getReferenceCorpusStoryId()}，切块元数据含 {@link RagMetadataKeys#SOURCE_REFERENCE}
 * 及分类字段，检索侧需 {@code erotica.rag.include-reference-corpus=true} 才会与用户故事一并召回。
 *
 * @author EroticaForge
 */
@Service
@RequiredArgsConstructor
public class CorpusJsonlReferenceImporter {

    private static final Logger log = LoggerFactory.getLogger(CorpusJsonlReferenceImporter.class);

    private final RagIngestionService ragIngestionService;
    private final ObjectMapper objectMapper;

    /**
     * 按行读取 JSONL，对每条在 {@code corpusRoot} 下解析 {@code source_relative_path} 并摄入向量库。
     *
     * @param jsonlPath       分类结果 JSONL（UTF-8）
     * @param corpusRoot      语料根目录（与数据分析批处理时的 input 根一致）
     * @param referenceStoryId 参考库 story_id（通常与配置 {@link com.eroticaforge.config.RagProperties#getReferenceCorpusStoryId()} 相同）
     * @return 统计
     */
    public ImportResult importFromJsonl(Path jsonlPath, Path corpusRoot, String referenceStoryId)
            throws IOException {
        if (!Files.isRegularFile(jsonlPath)) {
            throw new IllegalArgumentException("jsonl 不存在或不是文件: " + jsonlPath);
        }
        if (!Files.isDirectory(corpusRoot)) {
            throw new IllegalArgumentException("corpusRoot 必须是目录: " + corpusRoot);
        }
        Path rootNorm = corpusRoot.toAbsolutePath().normalize();
        ImportResult result = new ImportResult();
        try (Stream<String> lines = Files.lines(jsonlPath, StandardCharsets.UTF_8)) {
            lines.forEach(
                    line -> {
                        if (line.isBlank()) {
                            return;
                        }
                        try {
                            if (processLine(line, rootNorm, referenceStoryId)) {
                                result.imported++;
                            } else {
                                result.skipped++;
                            }
                        } catch (Exception ex) {
                            result.failed++;
                            log.warn("导入失败: {} — {}", truncate(line, 120), ex.getMessage());
                        }
                    });
        }
        log.info(
                "语料 JSONL 导入完成: imported={} skipped={} failed={}",
                result.imported,
                result.skipped,
                result.failed);
        return result;
    }

    private boolean processLine(String line, Path corpusRootNorm, String referenceStoryId) throws Exception {
        JsonNode n = objectMapper.readTree(line);
        String rel = n.path("source_relative_path").asText("");
        if (!StringUtils.hasText(rel)) {
            return false;
        }
        Path file = corpusRootNorm.resolve(rel).normalize();
        if (!file.startsWith(corpusRootNorm)) {
            log.warn("跳过非法路径（疑似穿越）: {}", rel);
            return false;
        }
        if (!Files.isRegularFile(file)) {
            log.warn("文件不存在: {}", file);
            return false;
        }
        if (!rel.toLowerCase().endsWith(".txt")) {
            log.debug("非 txt 跳过: {}", rel);
            return false;
        }

        byte[] bytes = Files.readAllBytes(file);
        String expectSha = n.path("content_sha256").asText("");
        if (StringUtils.hasText(expectSha)) {
            String actual = sha256Hex(bytes);
            if (!expectSha.equalsIgnoreCase(actual)) {
                log.warn("SHA256 不一致仍继续导入: {}", rel);
            }
        }

        String text = CorpusTextFiles.readAllText(file);
        if (!StringUtils.hasText(text)) {
            return false;
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        putIfText(meta, RagMetadataKeys.CORPUS_RELATIVE_PATH, rel);
        putIfText(meta, RagMetadataKeys.CORPUS_TITLE, n.path("title").asText(null));
        putIfText(meta, RagMetadataKeys.CORPUS_MAIN_TYPE, n.path("main_type").asText(null));
        JsonNode tags = n.get("tags");
        if (tags != null && tags.isArray() && !tags.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode t : tags) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(t.asText(""));
            }
            if (sb.length() > 0) {
                meta.put(RagMetadataKeys.CORPUS_TAGS, sb.toString());
            }
        }

        String label = n.path("title").asText("");
        if (!StringUtils.hasText(label)) {
            label = rel;
        }
        ragIngestionService.ingestCorpusReference(referenceStoryId, text, label, meta);
        return true;
    }

    private static void putIfText(Map<String, Object> meta, String key, String value) {
        if (StringUtils.hasText(value)) {
            meta.put(key, value);
        }
    }

    private static String sha256Hex(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(md.digest(bytes));
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    /**
     * 导入统计。
     *
     * @author EroticaForge
     */
    public static final class ImportResult {
        private int imported;
        private int skipped;
        private int failed;

        public int getImported() {
            return imported;
        }

        public int getSkipped() {
            return skipped;
        }

        public int getFailed() {
            return failed;
        }
    }
}
