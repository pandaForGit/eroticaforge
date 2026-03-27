package com.eroticaforge.dataanalysis.charactercard;

import com.eroticaforge.dataanalysis.charactercard.model.CharacterCardFileOutput;
import com.eroticaforge.dataanalysis.charactercard.model.CharacterCardLlmEnvelope;
import com.eroticaforge.dataanalysis.charactercard.model.CharacterCardTrigger;
import com.eroticaforge.dataanalysis.charactercard.model.ExtractedCharacterCard;
import com.eroticaforge.dataanalysis.charactercard.support.CharacterCardJsonRepair;
import com.eroticaforge.dataanalysis.charactercard.support.CharacterCardRichness;
import com.eroticaforge.dataanalysis.config.CharacterCardProcessingProperties;
import com.eroticaforge.dataanalysis.corpus.CorpusSanitizer;
import com.eroticaforge.dataanalysis.corpus.model.CorpusSanitizeOutcome;
import com.eroticaforge.dataanalysis.corpus.model.CorpusSkippedRecord;
import com.eroticaforge.dataanalysis.corpus.model.CorpusSkipReason;
import com.eroticaforge.dataanalysis.corpus.support.LlmJsonExtractor;
import com.eroticaforge.dataanalysis.corpus.support.OpenAiChatCompletionClient;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 从小说 txt 摘录中批量抽取主要角色人物卡，写入 JSONL（对齐 {@code docs/guides/高质量人物卡.md} 维度）。
 *
 * @author EroticaForge
 */
@Service
public class CharacterCardExtractorService {

    private static final Logger log = LoggerFactory.getLogger(CharacterCardExtractorService.class);

    private static final String SKIP_PARSE = "CHARACTER_CARD_JSON_PARSE";

    /** 根对象上的 {@code characters} 键（避免正文里出现单词 characters 误加分）。 */
    private static final Pattern CHARACTERS_ROOT_KEY =
            Pattern.compile("\"characters\"\\s*:|'characters'\\s*:");

    /** 通过独立 system 消息压低「Thinking Process」类输出（llama-server 通常支持 messages[0]=system）。 */
    private static final String CHARACTER_CARD_SYSTEM_PROMPT =
            "你是结构化抽取接口。禁止输出思考过程、英文分析、Markdown、代码围栏、“Thinking Process”等标题；"
                    + "禁止重复同一句或同一列表，禁止 “Okay, I'll pick”“Wait, I need to check” 等英文自我循环；"
                    + "推理只在内部完成；回复正文必须仅为一段合法 JSON，第一个非空白字符必须是 {；"
                    + "所有键名必须用英文双引号包裹（禁止 JavaScript 简写如 {name, age} 或无冒号的逗号分隔）；"
                    + "字段内容尽量简短，勿展开长段落。";

    private final OpenAiChatCompletionClient chatCompletionClient;
    private final CorpusSanitizer corpusSanitizer;
    private final CharacterCardProcessingProperties cardProperties;
    private final ObjectMapper objectMapper;

    public CharacterCardExtractorService(
            OpenAiChatCompletionClient chatCompletionClient,
            CorpusSanitizer corpusSanitizer,
            CharacterCardProcessingProperties cardProperties) {
        this.chatCompletionClient = chatCompletionClient;
        this.corpusSanitizer = corpusSanitizer;
        this.cardProperties = cardProperties;
        JsonFactory cardJsonFactory =
                JsonFactory.builder()
                        .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
                        .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                        .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                        .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                        .build();
        this.objectMapper = new ObjectMapper(cardJsonFactory);
        this.objectMapper.findAndRegisterModules();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 递归处理目录下 .txt，每文件一行 JSONL（含多角色数组）。
     *
     * @param inputRoot   语料根目录
     * @param outputJsonl 输出文件（追加）
     * @return 统计
     */
    public BatchStats extractDirectoryToJsonl(Path inputRoot, Path outputJsonl) throws IOException {
        if (!Files.isDirectory(inputRoot)) {
            throw new IllegalArgumentException("not a directory: " + inputRoot);
        }
        Path parent = outputJsonl.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path skippedPath = cardProperties.resolveSkippedJsonlPath(outputJsonl);
        if (skippedPath != null && skippedPath.getParent() != null) {
            Files.createDirectories(skippedPath.getParent());
        }

        Set<String> doneKeys = new HashSet<>();
        if (cardProperties.isResumeEnabled()) {
            loadDoneKeys(outputJsonl, doneKeys);
            if (skippedPath != null && Files.exists(skippedPath)) {
                loadDoneKeys(skippedPath, doneKeys);
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
            BufferedWriter skipWriter = openSkipped(skippedPath);
            try {
                paths.filter(Files::isRegularFile)
                        .filter(CharacterCardExtractorService::isTxt)
                        .sorted()
                        .forEachOrdered(
                                path -> {
                                    try {
                                        Outcome o =
                                                processOne(
                                                        inputRoot,
                                                        path,
                                                        outWriter,
                                                        skipWriter,
                                                        doneKeys);
                                        switch (o) {
                                            case SUCCESS -> stats.success++;
                                            case SKIPPED_SANITIZE -> stats.skipped++;
                                            case RESUMED -> stats.resumed++;
                                        }
                                    } catch (Exception ex) {
                                        stats.failed++;
                                        log.warn(
                                                "character card failed: {} — {}",
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
                "character card extract done: success={}, skipped={}, failed={}, resumed={}",
                stats.success,
                stats.skipped,
                stats.failed,
                stats.resumed);
        return stats;
    }

    private BufferedWriter openSkipped(Path skippedPath) throws IOException {
        if (skippedPath == null) {
            return null;
        }
        return Files.newBufferedWriter(
                skippedPath,
                StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
    }

    private void loadDoneKeys(Path file, Set<String> keys) {
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
                                JsonNode n = objectMapper.readTree(line);
                                String rel = n.path("source_relative_path").asText("");
                                String hash = n.path("content_sha256").asText("");
                                if (!rel.isEmpty() && !hash.isEmpty()) {
                                    keys.add(rel + "\t" + hash);
                                }
                            } catch (Exception ex) {
                                log.warn("ignore bad character-card jsonl line: {}", ex.getMessage());
                            }
                        });
            }
        } catch (IOException ex) {
            log.warn("could not read {}: {}", file, ex.getMessage());
        }
    }

    private Outcome processOne(
            Path inputRoot,
            Path file,
            BufferedWriter outWriter,
            BufferedWriter skipWriter,
            Set<String> doneKeys)
            throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        String sha256 = sha256Hex(bytes);
        String relative = inputRoot.relativize(file).toString().replace('\\', '/');
        String key = relative + "\t" + sha256;

        if (cardProperties.isResumeEnabled() && doneKeys.contains(key)) {
            log.debug("resume skip character card: {}", displayPath(inputRoot, file));
            return Outcome.RESUMED;
        }

        if (cardProperties.isLogTrace()) {
            log.info("[人物卡追踪] 开始处理 {} fileBytes={}", displayPath(inputRoot, file), bytes.length);
        }

        CorpusSanitizer.Result sanitize = corpusSanitizer.sanitizeBytes(bytes);
        if (!sanitize.isOk()) {
            if (cardProperties.isLogTrace()) {
                log.info(
                        "[人物卡追踪] {} 清洗跳过 reason={}",
                        displayPath(inputRoot, file),
                        sanitize.skipReason());
            }
            appendSkip(
                    skipWriter,
                    relative,
                    sha256,
                    CorpusSkippedRecord.of(relative, sha256, sanitize.skipReason()),
                    doneKeys,
                    key);
            return Outcome.SKIPPED_SANITIZE;
        }

        CorpusSanitizeOutcome outcome = sanitize.outcome();
        String display = displayPath(inputRoot, file);
        if (cardProperties.isLogTrace()) {
            log.info(
                    "[人物卡追踪] {} 清洗后正文 chars={}",
                    display,
                    outcome.utf16Text().length());
        }

        String prompt =
                buildExtractionPromptWithinBudget(
                        file.getFileName().toString(),
                        relative,
                        outcome.utf16Text());

        if (cardProperties.isLogTrace()) {
            log.info("[人物卡追踪] {} prompt.chars={}\n{}", display, prompt.length(), ellipsizeMiddle(prompt, 3500));
        }

        String raw = invokeLlmWithRetries(prompt, display);

        List<ExtractedCharacterCard> characters;
        try {
            characters = parseCharacterEnvelopeFromLlmRaw(raw, display);
        } catch (Exception ex) {
            appendSkip(
                    skipWriter,
                    relative,
                    sha256,
                    CorpusSkippedRecord.ofCustom(relative, sha256, SKIP_PARSE + ": " + ex.getMessage()),
                    doneKeys,
                    key);
            log.warn("parse character card JSON failed: {} — {}", display, ex.getMessage());
            logCharacterCardParseFailure(display, raw, ex);
            return Outcome.SKIPPED_SANITIZE;
        }

        characters = CharacterCardRichness.filterRichCards(characters);

        if (characters == null || characters.isEmpty()) {
            appendSkip(
                    skipWriter,
                    relative,
                    sha256,
                    CorpusSkippedRecord.ofCustom(relative, sha256, "CHARACTER_CARD_SHELL_CARDS"),
                    doneKeys,
                    key);
            log.warn(
                    "character card rejected (only shell fields e.g. name-only): {}",
                    display);
            logCharacterCardParseFailure(display, raw, new IllegalArgumentException("CHARACTER_CARD_SHELL_CARDS"));
            return Outcome.SKIPPED_SANITIZE;
        }

        int cap = cardProperties.getMaxRolesPerFile();
        if (characters.size() > cap) {
            characters = characters.subList(0, cap);
        }

        CharacterCardFileOutput line = CharacterCardFileOutput.of(relative, sha256, characters);
        outWriter.write(objectMapper.writeValueAsString(line));
        outWriter.newLine();
        outWriter.flush();
        doneKeys.add(key);
        log.info(
                "character cards [{}] roles={}",
                displayPath(inputRoot, file),
                characters.size());
        return Outcome.SUCCESS;
    }

    private void appendSkip(
            BufferedWriter skipWriter,
            String relative,
            String sha256,
            CorpusSkippedRecord record,
            Set<String> doneKeys,
            String key)
            throws IOException {
        if (skipWriter == null) {
            return;
        }
        skipWriter.write(objectMapper.writeValueAsString(record));
        skipWriter.newLine();
        skipWriter.flush();
        doneKeys.add(key);
    }

    /**
     * 模型常夹杂说明文字、markdown、无引号键名；按候选块依次尝试解析。
     */
    private List<ExtractedCharacterCard> parseCharacterEnvelopeFromLlmRaw(String raw, String displayPath)
            throws Exception {
        boolean trace = cardProperties.isLogTrace();
        if (trace) {
            log.info("[人物卡追踪] {} 模型原文 raw.chars={}", displayPath, raw == null ? -1 : raw.length());
        }
        List<String> candidates = new ArrayList<>(LlmJsonExtractor.extractJsonObjectsBroad(raw));
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("no JSON object in model output");
        }
        candidates.sort(
                Comparator.comparingInt((String j) -> scoreCharacterJsonCandidate(j))
                        .reversed()
                        .thenComparingInt(String::length)
                        .reversed());
        if (trace) {
            log.info("[人物卡追踪] {} 平衡括号截取候选数={}", displayPath, candidates.size());
            for (int i = 0; i < candidates.size(); i++) {
                String c = candidates.get(i);
                log.info(
                        "[人物卡追踪] {} 候选#{} len={} score={} head={}",
                        displayPath,
                        i,
                        c.length(),
                        scoreCharacterJsonCandidate(c),
                        ellipsizeMiddle(c, 400));
            }
        }
        Exception last = null;
        int idx = 0;
        for (String json : candidates) {
            String trimmed = json.trim();
            if (trimmed.startsWith("[")) {
                String arr =
                        CharacterCardJsonRepair.trimGarbageAfterStructuralOpen(
                                LlmJsonExtractor.normalizeArrayAfterOpeningBracket(trimmed));
                List<ExtractedCharacterCard> fromArray = tryParseCharactersRootArray(arr);
                if (fromArray != null && CharacterCardRichness.anyRichCard(fromArray)) {
                    if (trace) {
                        log.info("[人物卡追踪] {} 候选#{} 按根数组解析成功 roles={}", displayPath, idx, fromArray.size());
                    }
                    return fromArray;
                }
                if (fromArray != null && trace) {
                    log.info(
                            "[人物卡追踪] {} 候选#{} 根数组反序列化成功但无有效角色字段，忽略",
                            displayPath,
                            idx);
                }
                if (trace) {
                    log.info("[人物卡追踪] {} 候选#{} 根数组解析未得到非空角色列表", displayPath, idx);
                }
                idx++;
                continue;
            }
            String normalized =
                    CharacterCardJsonRepair.trimGarbageAfterStructuralOpen(
                            LlmJsonExtractor.normalizeObjectAfterOpeningBrace(trimmed));
            List<ExtractedCharacterCard> envelopeList = tryParseEnvelopeAllStrategies(normalized);
            if (envelopeList != null) {
                if (trace) {
                    log.info("[人物卡追踪] {} 候选#{} 按 envelope 解析成功 roles={}", displayPath, idx, envelopeList.size());
                }
                return envelopeList;
            }
            try {
                objectMapper.readValue(
                        CharacterCardJsonRepair.applyStandardRepairs(normalized),
                        CharacterCardLlmEnvelope.class);
            } catch (Exception ex) {
                last = ex;
                if (trace) {
                    log.info("[人物卡追踪] {} 候选#{} envelope 解析异常: {}", displayPath, idx, ex.getMessage());
                }
            }
            idx++;
        }
        if (last != null) {
            throw last;
        }
        throw new IllegalArgumentException("characters array missing or empty in all JSON candidates");
    }

    /** 解析失败时的 WARN 诊断（与 {@code erotica.character-card.log-trace} 无关，便于对照 skipped jsonl）。 */
    private void logCharacterCardParseFailure(String displayPath, String raw, Exception ex) {
        int max = cardProperties.getLogRawResponseOnFailureMaxChars();
        if (max <= 0) {
            max = 12000;
        }
        max = Math.min(max, 100_000);
        log.warn("[人物卡诊断] {} failureClass={} message={}", displayPath, ex.getClass().getSimpleName(), ex.getMessage());
        if (raw == null) {
            log.warn("[人物卡诊断] {} 模型输出为 null", displayPath);
            return;
        }
        log.warn("[人物卡诊断] {} 原始模型输出 raw.length={}", displayPath, raw.length());
        log.warn("[人物卡诊断] {} 原始输出 headTail:\n{}", displayPath, ellipsizeMiddle(raw, max));
        try {
            String prep = LlmJsonExtractor.preprocess(raw);
            int opens = countChar(prep, '{');
            int closes = countChar(prep, '}');
            log.warn(
                    "[人物卡诊断] {} 预处理后 length={} `{`={} `}`={}",
                    displayPath,
                    prep.length(),
                    opens,
                    closes);
            log.warn("[人物卡诊断] {} 预处理后 headTail:\n{}", displayPath, ellipsizeMiddle(prep, max));
        } catch (Exception pex) {
            log.warn("[人物卡诊断] {} 预处理失败（无法剥离围栏）: {}", displayPath, pex.getMessage());
        }
    }

    private static int countChar(String s, char ch) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ch) {
                n++;
            }
        }
        return n;
    }

    /** 头尾各取约一半预算，中间省略，便于日志中同时看到开场白与末尾 JSON。 */
    private static String ellipsizeMiddle(String s, int maxChars) {
        if (s == null) {
            return "null";
        }
        if (s.length() <= maxChars) {
            return s;
        }
        int head = maxChars / 2;
        int tail = maxChars - head;
        int omitted = s.length() - head - tail;
        return s.substring(0, head) + "\n... （省略 " + omitted + " 字符）...\n" + s.substring(s.length() - tail);
    }

    /**
     * 优先含根键 {@code "characters":} 的完整对象；压低仅 triggers 形态的小数组（易误解析成空壳角色）。
     */
    private static int scoreCharacterJsonCandidate(String json) {
        if (json == null) {
            return 0;
        }
        int score = 0;
        if (json.contains("[...]")
                || json.contains("{...}")
                || json.contains("\", ...")) {
            score -= 450;
        }
        if (CHARACTERS_ROOT_KEY.matcher(json).find()) {
            score += 200;
        }
        if (CHARACTERS_ROOT_KEY.matcher(json).find()
                && (json.contains("\"name\"") || json.contains("'name'"))) {
            score += 130;
        }
        String t = json.trim();
        if (t.startsWith("[")) {
            if (t.contains("\"keyword\"")
                    && t.contains("\"reaction\"")
                    && !t.contains("\"name\"")
                    && !t.contains("'name'")) {
                score -= 160;
            }
            if (t.contains("\"name\"") || t.contains("'name'")) {
                score += 70;
            }
        }
        String compactHead = json.substring(0, Math.min(json.length(), 96)).replaceAll("\\s+", "");
        if (compactHead.startsWith("{characters") || compactHead.startsWith("{'characters'")) {
            score += 50;
        }
        return score;
    }

    /**
     * envelope：标准反序列化 → 去串外省略点 → {@link JsonNode} 逐条映射（容忍键附近少量垃圾字符）。
     */
    private List<ExtractedCharacterCard> tryParseEnvelopeAllStrategies(String normalized) {
        String core = CharacterCardJsonRepair.applyStandardRepairs(normalized);
        List<ExtractedCharacterCard> r = tryEnvelopeBeanParse(core);
        if (r != null) {
            return r;
        }
        r = tryEnvelopeTreeParse(core);
        return r;
    }

    private List<ExtractedCharacterCard> tryEnvelopeBeanParse(String json) {
        try {
            CharacterCardLlmEnvelope env = objectMapper.readValue(json, CharacterCardLlmEnvelope.class);
            List<ExtractedCharacterCard> list = env.getCharacters();
            if (list != null && !list.isEmpty() && CharacterCardRichness.anyRichCard(list)) {
                return list;
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }

    private List<ExtractedCharacterCard> tryEnvelopeTreeParse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                return null;
            }
            JsonNode arr = root.get("characters");
            if (arr == null || !arr.isArray() || arr.isEmpty()) {
                return null;
            }
            List<ExtractedCharacterCard> list = new ArrayList<>();
            for (JsonNode n : arr) {
                if (!n.isObject()) {
                    continue;
                }
                ExtractedCharacterCard c = mapCharacterObjectNode(n);
                if (c != null) {
                    list.add(c);
                }
            }
            if (CharacterCardRichness.anyRichCard(list)) {
                return list;
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }

    private ExtractedCharacterCard mapCharacterObjectNode(JsonNode n) {
        try {
            return objectMapper.treeToValue(n, ExtractedCharacterCard.class);
        } catch (Exception ignored) {
            // fall through
        }
        try {
            return objectMapper.convertValue(n, ExtractedCharacterCard.class);
        } catch (Exception ignored) {
            // fall through
        }
        return mapCharacterObjectNodeLenient(n);
    }

    private static String nodeText(JsonNode parent, String field) {
        JsonNode v = parent.get(field);
        if (v == null || v.isNull()) {
            return "";
        }
        if (v.isTextual()) {
            return v.asText();
        }
        if (v.isNumber() || v.isBoolean()) {
            return v.asText();
        }
        return v.toString();
    }

    /** 字段级回退（嵌套 JSON 等非标准标量仍可能失败，此时返回 null）。 */
    private static ExtractedCharacterCard mapCharacterObjectNodeLenient(JsonNode n) {
        ExtractedCharacterCard c = new ExtractedCharacterCard();
        c.setName(nodeText(n, "name"));
        c.setAge(nodeText(n, "age"));
        c.setIdentity(nodeText(n, "identity"));
        c.setAppearance(nodeText(n, "appearance"));
        c.setPersonality(nodeText(n, "personality"));
        c.setNsfwProfile(nodeText(n, "nsfw_profile"));
        c.setPsychologyAndRelations(nodeText(n, "psychology_and_relations"));
        c.setBackground(nodeText(n, "background"));
        JsonNode tr = n.get("triggers");
        if (tr != null && tr.isArray()) {
            List<CharacterCardTrigger> triggers = new ArrayList<>();
            for (JsonNode t : tr) {
                if (t != null && t.isObject()) {
                    CharacterCardTrigger ct = new CharacterCardTrigger();
                    ct.setKeyword(nodeText(t, "keyword"));
                    ct.setReaction(nodeText(t, "reaction"));
                    triggers.add(ct);
                }
            }
            c.setTriggers(triggers);
        }
        JsonNode sd = n.get("sample_dialogues");
        if (sd != null && sd.isArray()) {
            List<String> d = new ArrayList<>();
            for (JsonNode x : sd) {
                if (x != null && !x.isNull()) {
                    if (x.isTextual()) {
                        d.add(x.asText());
                    } else {
                        d.add(x.asText(""));
                    }
                }
            }
            c.setSampleDialogues(d);
        }
        if (!c.getName().isBlank()
                || !c.getIdentity().isBlank()
                || !c.getAppearance().isBlank()
                || !c.getPersonality().isBlank()
                || !c.getBackground().isBlank()
                || !c.getNsfwProfile().isBlank()
                || !c.getPsychologyAndRelations().isBlank()) {
            return c;
        }
        return null;
    }

    /**
     * 模型偶发直接输出角色数组 {@code [{...},...]}，包一层再反序列化。
     */
    private List<ExtractedCharacterCard> tryParseCharactersRootArray(String normalized) {
        String t =
                CharacterCardJsonRepair.applyStandardRepairs(
                        CharacterCardJsonRepair.trimGarbageAfterStructuralOpen(normalized.trim()));
        if (t.length() < 2 || t.charAt(0) != '[') {
            return null;
        }
        List<ExtractedCharacterCard> list = tryParseRootArrayBean(t);
        if (list != null && !list.isEmpty()) {
            return list;
        }
        list = tryParseRootArrayTree(t);
        return list;
    }

    private List<ExtractedCharacterCard> tryParseRootArrayBean(String json) {
        try {
            List<ExtractedCharacterCard> list =
                    objectMapper.readValue(
                            json,
                            objectMapper
                                    .getTypeFactory()
                                    .constructCollectionType(List.class, ExtractedCharacterCard.class));
            if (list != null && !list.isEmpty()) {
                return list;
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }

    private List<ExtractedCharacterCard> tryParseRootArrayTree(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray() || root.isEmpty()) {
                return null;
            }
            List<ExtractedCharacterCard> list = new ArrayList<>();
            for (JsonNode n : root) {
                if (!n.isObject()) {
                    continue;
                }
                ExtractedCharacterCard c = mapCharacterObjectNode(n);
                if (c != null) {
                    list.add(c);
                }
            }
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }

    private String buildExtractionPromptWithinBudget(String fileName, String relativePath, String fullCleaned) {
        int minEx = cardProperties.getMinExcerptChars();
        int maxTotal = cardProperties.getMaxTotalPromptChars();
        int cap = cardProperties.getMaxExcerptChars();
        int excerptLen = Math.min(fullCleaned.length(), cap);
        String excerpt = fullCleaned.substring(0, excerptLen);
        int maxRoles = cardProperties.getMaxRolesPerFile();
        while (true) {
            String prompt = buildExtractionPrompt(fileName, relativePath, excerpt, maxRoles);
            if (prompt.length() <= maxTotal) {
                return prompt;
            }
            if (excerpt.length() <= minEx) {
                log.warn(
                        "character card prompt length {} > maxTotal {}; context may truncate",
                        prompt.length(),
                        maxTotal);
                return prompt;
            }
            int next = Math.max(minEx, excerpt.length() * 3 / 4);
            excerpt = fullCleaned.substring(0, Math.min(next, fullCleaned.length()));
        }
    }

    private static String buildExtractionPrompt(
            String fileName, String relativePath, String excerpt, int maxRoles) {
        return """
                根据下列小说摘录，抽取最多 %d 名主要角色，**直接输出且仅输出**一段合法 JSON（第一个非空白字符为 {）。不要 markdown、不要说明、不要任何英文分析段落。
                **禁止** “Thinking Process”“Analyze”“Drafting” 等标题或步骤列表；**禁止**在 JSON 内用 [...]、{...}、… 占位。

                根对象：{"characters":[ ... ]}。每个角色对象字段（缺失写 ""；字符串宜短，勿写长文）：
                name, age, identity, appearance, personality, nsfw_profile, psychology_and_relations, background,
                triggers（0～2 项即可，每项 keyword、reaction 各为短字符串）,
                sample_dialogues（**每角色 1～2 条**短台词即可，勿凑条数、勿重复列举）。

                文件名：%s
                路径：%s
                正文摘录：
                %s
                """
                .formatted(maxRoles, fileName, relativePath, excerpt);
    }

    private String invokeLlmWithRetries(String prompt, String displayPath) throws IOException {
        int maxAttempts = Math.max(1, cardProperties.getLlmEmptyResponseRetries() + 1);
        long delayMs = Math.max(0L, cardProperties.getLlmRetryDelayMs());
        IOException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (cardProperties.isLogTrace()) {
                log.info("[人物卡追踪] {} 调用 LLM attempt={}/{}", displayPath, attempt, maxAttempts);
            }
            try {
                int cap = cardProperties.getLlmMaxOutputTokens();
                OpenAiChatCompletionClient.ChatCompletion completion =
                        chatCompletionClient.completeWithSystemAndLimits(
                                CHARACTER_CARD_SYSTEM_PROMPT,
                                prompt,
                                cap,
                                cardProperties.getLlmTemperature(),
                                cardProperties.isLlmUseOpenAiStyleSampling(),
                                cardProperties.getLlmTopP(),
                                cardProperties.getLlmFrequencyPenalty(),
                                cardProperties.getLlmPresencePenalty());
                String text = completion.text();
                String finish = completion.finishReason();
                if (finish != null
                        && (finish.equalsIgnoreCase("length")
                                || finish.equalsIgnoreCase("max_tokens"))) {
                    log.warn(
                            "character card LLM finish_reason={}（输出可能在 JSON 中间被截断）：可增大 erotica.character-card.llm-max-output-tokens、在服务端关思考/提高 n_ctx、或加大 erotica.character-card.llm-frequency-penalty 打断复读；{}",
                            finish,
                            displayPath);
                } else if (cardProperties.isLogTrace()) {
                    log.info("[人物卡追踪] {} LLM finish_reason={}", displayPath, finish);
                }
                if (text != null && !text.isBlank()) {
                    if (cardProperties.isLogTrace()) {
                        log.info(
                                "[人物卡追踪] {} LLM 返回 chars={}\n{}",
                                displayPath,
                                text.length(),
                                ellipsizeMiddle(text, 4500));
                    }
                    return text;
                }
                log.warn(
                        "character card LLM empty (attempt {}/{}), promptChars={}",
                        attempt,
                        maxAttempts,
                        prompt.length());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException("interrupted", ex);
            } catch (IOException ex) {
                last = ex;
                log.warn("character card LLM (attempt {}): {}", attempt, ex.getMessage());
            } catch (Exception ex) {
                last = new IOException(ex.getMessage(), ex);
                log.warn("character card LLM (attempt {}): {}", attempt, ex.getMessage());
            }
            if (attempt < maxAttempts && delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("interrupted during retry", ie);
                }
            }
        }
        if (last != null) {
            throw last;
        }
        throw new IOException("empty LLM output after retries");
    }

    private String displayPath(Path inputRoot, Path file) {
        if (cardProperties.isLogFullPaths()) {
            return file.toAbsolutePath().toString();
        }
        return inputRoot.relativize(file).toString().replace('\\', '/');
    }

    private static boolean isTxt(Path p) {
        return p.getFileName().toString().toLowerCase().endsWith(".txt");
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private enum Outcome {
        SUCCESS,
        SKIPPED_SANITIZE,
        RESUMED
    }

    /**
     * 批处理统计。
     *
     * @author EroticaForge
     */
    public static final class BatchStats {
        private int success;
        private int skipped;
        private int failed;
        private int resumed;

        public int getSuccess() {
            return success;
        }

        public int getSkipped() {
            return skipped;
        }

        public int getFailed() {
            return failed;
        }

        public int getResumed() {
            return resumed;
        }
    }
}
