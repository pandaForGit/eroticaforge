package com.eroticaforge.application.service;

import com.eroticaforge.infrastructure.persistence.CharacterLibraryRepository;
import com.eroticaforge.infrastructure.persistence.entity.CharacterLibraryEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 将数据分析模块产出的人物卡 JSONL 导入 {@code erotica_character_library}。
 *
 * @author EroticaForge
 */
@Service
@RequiredArgsConstructor
public class CharacterCardsJsonlImporter {

    private static final Logger log = LoggerFactory.getLogger(CharacterCardsJsonlImporter.class);

    private final CharacterLibraryRepository characterLibraryRepository;
    private final ObjectMapper objectMapper;

    /**
     * 导入统计。
     */
    @Getter
    public static class ImportResult {
        private int inserted;
        private int updated;
        private int skipped;
        private int failed;

        /**
         * 记录新增。
         */
        void incInserted() {
            inserted++;
        }

        /**
         * 记录更新。
         */
        void incUpdated() {
            updated++;
        }

        /**
         * 记录跳过。
         */
        void incSkipped() {
            skipped++;
        }

        /**
         * 记录失败。
         */
        void incFailed() {
            failed++;
        }
    }

    /**
     * 从 UTF-8 JSONL 文件导入。
     *
     * @param jsonlPath JSONL 路径
     * @return 统计结果
     */
    public ImportResult importFromJsonl(Path jsonlPath) {
        ImportResult result = new ImportResult();
        if (!Files.isRegularFile(jsonlPath)) {
            log.warn("Character cards JSONL 不存在，跳过导入: {}", jsonlPath.toAbsolutePath());
            return result;
        }
        try {
            try (var lines = Files.lines(jsonlPath, StandardCharsets.UTF_8)) {
                Iterator<String> it = lines.iterator();
                int lineNo = 0;
                while (it.hasNext()) {
                    lineNo++;
                    String line = it.next();
                    if (line == null || line.isBlank()) {
                        continue;
                    }
                    try {
                        processLine(line.strip(), result);
                    } catch (Exception ex) {
                        result.incFailed();
                        log.warn("人物卡 JSONL 第 {} 行解析失败: {}", lineNo, ex.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            log.error("读取人物卡 JSONL 失败: {}", jsonlPath, ex);
            throw new IllegalStateException("读取人物卡 JSONL 失败: " + ex.getMessage(), ex);
        }
        return result;
    }

    private void processLine(String line, ImportResult result) throws Exception {
        JsonNode root = objectMapper.readTree(line);
        String sha = textOrEmpty(root, "content_sha256");
        if (!StringUtils.hasText(sha)) {
            result.incSkipped();
            return;
        }
        JsonNode chars = root.get("characters");
        if (chars == null || !chars.isArray() || chars.isEmpty()) {
            result.incSkipped();
            return;
        }
        String sourcePath = textOrEmpty(root, "source_relative_path");
        String schemaVersion = textOrEmpty(root, "schema_version");
        if (!StringUtils.hasText(schemaVersion)) {
            schemaVersion = "1";
        }
        Instant now = Instant.now();
        for (int i = 0; i < chars.size(); i++) {
            JsonNode ch = chars.get(i);
            if (ch == null || !ch.isObject()) {
                result.incSkipped();
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> payload =
                    objectMapper.convertValue(ch, LinkedHashMap.class);
            String displayName = textOrEmpty(ch, "name");
            CharacterLibraryEntity row = new CharacterLibraryEntity();
            row.setId(UUID.randomUUID().toString());
            row.setSchemaVersion(schemaVersion);
            row.setSourceRelativePath(sourcePath);
            row.setContentSha256(sha.strip());
            row.setRoleIndex(i);
            row.setDisplayName(displayName);
            row.setPayload(payload);
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            boolean inserted = characterLibraryRepository.insertOrUpdateByShaAndRole(row);
            if (inserted) {
                result.incInserted();
            } else {
                result.incUpdated();
            }
        }
    }

    private static String textOrEmpty(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return n == null || n.isNull() ? "" : n.asText("").strip();
    }
}
