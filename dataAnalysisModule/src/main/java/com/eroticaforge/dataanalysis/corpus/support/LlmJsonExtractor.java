package com.eroticaforge.dataanalysis.corpus.support;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从模型原文中截取 JSON 对象（去除 markdown 代码围栏等）。
 *
 * <p>不用「第一个 '{' 到最后一个 '}'」切片，避免正文里出现花括号时截进杂质；改为对每个 '{' 做括号平衡，取<strong>最长</strong>的一段（通常为最外层完整对象）。
 *
 * @author EroticaForge
 */
public final class LlmJsonExtractor {

    private LlmJsonExtractor() {}

    /**
     * 去掉常见 markdown 围栏后，取<strong>最长</strong>的一段平衡 {...} 子串。
     *
     * @param raw 模型原始输出
     * @return JSON 对象字符串
     * @throws IllegalArgumentException 无法解析出对象时
     */
    public static String extractJsonObject(String raw) {
        List<String> all = extractJsonObjects(raw);
        if (all.isEmpty()) {
            throw new IllegalArgumentException("no JSON object in model output");
        }
        return all.get(0);
    }

    /**
     * 所有平衡 {...} 候选，按长度降序、同长度则起始位置靠后优先（更接近回复末尾的 JSON）。
     */
    public static List<String> extractJsonObjects(String raw) {
        return extractJsonObjectsFromNormalizedString(preprocess(raw));
    }

    /**
     * 合并「正常预处理」与「不切 Thinking 尾」两路候选，减少漏掉文末 JSON 或误切的情况。
     */
    public static List<String> extractJsonObjectsBroad(String raw) {
        LinkedHashSet<String> all = new LinkedHashSet<>();
        try {
            all.addAll(extractJsonObjectsFromNormalizedString(preprocess(raw)));
        } catch (IllegalArgumentException ignored) {
            // blank
        }
        try {
            if (raw != null && !raw.isBlank()) {
                all.addAll(extractJsonObjectsFromNormalizedString(preprocessWithoutThinkingSlice(raw)));
            }
        } catch (IllegalArgumentException ignored) {
            // blank
        }
        return new ArrayList<>(all);
    }

    static List<String> extractJsonObjectsFromNormalizedString(String s) {
        List<Candidate> cands = new ArrayList<>();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '{' && ch != '[') {
                continue;
            }
            String bal = extractBalancedJsonValue(s, i);
            if (bal != null && bal.length() > 2) {
                cands.add(new Candidate(bal, i));
            }
        }
        cands.sort(
                Comparator.comparingInt((Candidate c) -> c.text.length())
                        .reversed()
                        .thenComparingInt(c -> c.startIndex));
        List<String> dedup = new ArrayList<>();
        for (Candidate c : cands) {
            if (!dedup.contains(c.text)) {
                dedup.add(c.text);
            }
        }
        return dedup;
    }

    /**
     * 与 {@link #extractJsonObjects} 相同的围栏剥离与清理，便于日志诊断。
     *
     * @throws IllegalArgumentException 空输出
     */
    public static String preprocess(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("empty model output");
        }
        String s = raw.trim();
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
            s = s.substring(1).trim();
        }
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            if (firstNl > 0) {
                s = s.substring(firstNl + 1);
            } else {
                s = s.substring(3);
            }
            s = s.trim();
            int fence = s.lastIndexOf("```");
            if (fence >= 0) {
                s = s.substring(0, fence).trim();
            }
        }
        s = stripOuterBackticks(s);
        s = s.trim();
        if (s.toLowerCase().startsWith("json")) {
            int nl = s.indexOf('\n');
            if (nl > 0 && nl < 12) {
                s = s.substring(nl + 1).trim();
            } else if (s.length() > 4 && Character.isWhitespace(s.charAt(4))) {
                s = s.substring(4).trim();
            }
        }
        s = stripOuterBackticks(s).trim();
        s = maybeSliceFromLastCharactersRootObject(s);
        s = stripOuterBackticks(s).trim();
        return stripLeadingInvisible(s);
    }

    /**
     * 与 {@link #preprocess} 相同但不执行 {@link #maybeSliceFromLastCharactersRootObject}，用于与 {@link #preprocess} 互补搜候选。
     */
    public static String preprocessWithoutThinkingSlice(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("empty model output");
        }
        String s = raw.trim();
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
            s = s.substring(1).trim();
        }
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            if (firstNl > 0) {
                s = s.substring(firstNl + 1);
            } else {
                s = s.substring(3);
            }
            s = s.trim();
            int fence = s.lastIndexOf("```");
            if (fence >= 0) {
                s = s.substring(0, fence).trim();
            }
        }
        s = stripOuterBackticks(s);
        s = s.trim();
        if (s.toLowerCase().startsWith("json")) {
            int nl = s.indexOf('\n');
            if (nl > 0 && nl < 12) {
                s = s.substring(nl + 1).trim();
            } else if (s.length() > 4 && Character.isWhitespace(s.charAt(4))) {
                s = s.substring(4).trim();
            }
        }
        s = stripOuterBackticks(s).trim();
        return stripLeadingInvisible(s);
    }

    /**
     * 仅从「真实」人物卡根对象起截断：{@code "characters": [} 后必须紧跟角色对象 {@code \{}，排除思考稿里的
     * {@code {"characters": [...]}}、{@code `{"characters": [...]}`} 等占位符（否则会丢掉文末真 JSON）。
     */
    private static final Pattern REAL_CHARACTERS_ENVELOPE_HEAD =
            Pattern.compile("\\{\\s*\"characters\"\\s*:\\s*\\[\\s*\\{");
    private static final Pattern REAL_CHARACTERS_ENVELOPE_HEAD_SQ =
            Pattern.compile("\\{\\s*'characters'\\s*:\\s*\\[\\s*\\{");

    static String maybeSliceFromLastCharactersRootObject(String s) {
        if (s == null || s.length() < 300) {
            return s;
        }
        String low = s.toLowerCase(Locale.ROOT);
        if (!low.contains("thinking")
                && !low.contains("analyze the request")
                && !low.contains("drafting character")
                && !low.contains("思考过程")
                && !low.contains("分析请求")) {
            return s;
        }
        int cut = -1;
        cut = Math.max(cut, lastPatternStart(s, REAL_CHARACTERS_ENVELOPE_HEAD));
        cut = Math.max(cut, lastPatternStart(s, REAL_CHARACTERS_ENVELOPE_HEAD_SQ));
        if (cut < 0 || cut >= s.length() - 20) {
            return s;
        }
        if (cut < 120) {
            return s;
        }
        return s.substring(cut);
    }

    private static int lastPatternStart(String s, Pattern p) {
        Matcher m = p.matcher(s);
        int last = -1;
        while (m.find()) {
            last = m.start();
        }
        return last;
    }

    /**
     * 去掉开头的 BOM / 零宽字符；若紧跟在 '{' 后也会被 {@link #normalizeObjectAfterOpeningBrace} 处理。
     */
    public static String stripLeadingInvisible(String s) {
        if (s == null) {
            return null;
        }
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\uFEFF'
                    || c == '\u200B'
                    || c == '\u200C'
                    || c == '\u200D'
                    || c == '\u2060') {
                i++;
                continue;
            }
            break;
        }
        return i == 0 ? s : s.substring(i);
    }

    private static String stripOuterBackticks(String s) {
        String t = s.trim();
        while (t.length() >= 2 && t.charAt(0) == '`' && t.charAt(t.length() - 1) == '`') {
            t = t.substring(1, t.length() - 1).trim();
        }
        return t;
    }

    /**
     * 从 {@code start} 起匹配完整 JSON 对象或数组（忽略双引号、单引号字符串内的括号；与 Jackson
     * {@code ALLOW_SINGLE_QUOTES} 一致，避免模型输出 {@code {'a':{...}}} 时括号平衡错位）。
     */
    static String extractBalancedJsonValue(String s, int start) {
        if (start < 0 || start >= s.length()) {
            return null;
        }
        char open = s.charAt(start);
        if (open != '{' && open != '[') {
            return null;
        }
        Deque<Character> stack = new ArrayDeque<>();
        stack.push(open == '{' ? '}' : ']');
        /** 0 = 不在字符串内；{@code '"'} 或 {@code '\''} 表示当前字符串定界符 */
        char stringQuote = 0;
        boolean escape = false;
        for (int i = start + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (stringQuote != 0) {
                if (c == '\\') {
                    escape = true;
                } else if (c == stringQuote) {
                    stringQuote = 0;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                stringQuote = c;
                continue;
            }
            if (c == '{') {
                stack.push('}');
                continue;
            }
            if (c == '[') {
                stack.push(']');
                continue;
            }
            if (c == '}' || c == ']') {
                if (stack.isEmpty() || stack.pop() != c) {
                    return null;
                }
                if (stack.isEmpty()) {
                    return s.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    /**
     * 去掉紧跟在 '{' 后的空白、反引号等杂质，避免模型输出 {@code {`"characters"} 这类片段。
     */
    public static String normalizeObjectAfterOpeningBrace(String json) {
        String s = stripLeadingInvisible(json);
        if (s == null || s.length() < 2 || s.charAt(0) != '{') {
            return s;
        }
        int i = 1;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' '
                    || c == '\t'
                    || c == '\n'
                    || c == '\r'
                    || c == '`'
                    || c == '\uFEFF'
                    || c == '\u200B'
                    || c == '\u200C'
                    || c == '\u200D'
                    || c == '\u2060') {
                i++;
                continue;
            }
            break;
        }
        if (i == 1) {
            return s;
        }
        return "{" + s.substring(i);
    }

    /**
     * 去掉紧跟在 '[' 后的空白、反引号等，避免模型输出 {@code [`{...}`]} 导致解析失败。
     */
    public static String normalizeArrayAfterOpeningBracket(String json) {
        String s = stripLeadingInvisible(json);
        if (s == null || s.length() < 2 || s.charAt(0) != '[') {
            return s;
        }
        int i = 1;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' '
                    || c == '\t'
                    || c == '\n'
                    || c == '\r'
                    || c == '`'
                    || c == '\uFEFF'
                    || c == '\u200B'
                    || c == '\u200C'
                    || c == '\u200D'
                    || c == '\u2060') {
                i++;
                continue;
            }
            break;
        }
        if (i == 1) {
            return s;
        }
        return "[" + s.substring(i);
    }

    private static String normalizeObjectAfterOpeningBraceLegacy(String json) {
        if (json == null || json.length() < 2 || json.charAt(0) != '{') {
            return json;
        }
        int i = 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '`' || c == '\uFEFF') {
                i++;
                continue;
            }
            break;
        }
        if (i == 1) {
            return json;
        }
        return "{" + json.substring(i);
    }

    private record Candidate(String text, int startIndex) {}
}
