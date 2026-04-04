package com.eroticaforge.dataanalysis.charactercard.support;

/**
 * 修复模型输出里常见的非合法 JSON 片段（仅处理双引号字符串外的区域）。
 *
 * @author EroticaForge
 */
public final class CharacterCardJsonRepair {

    private CharacterCardJsonRepair() {}

    /**
     * 人物卡解析前常用修复链：先处理 {@code "key": ...[} 类省略点，再去掉串外长串 {@code ...}。
     */
    public static String applyStandardRepairs(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return stripDotRunsOutsideDoubleQuotedStrings(stripDotEllipsisAfterColonsOutsideStrings(s));
    }

    /**
     * 双引号串外：在 {@code :} 与下一个值之间若出现空白 + 句点/省略号，且紧随其后的非空白为数组/对象/字符串起始符
     * （{@code [}、左花括号、{@code "}），则去掉中间空白与句点（修复 {@code "characters":.[} 等）。
     * 不处理 {@code "x": .5} 等以小数点开头的数字，避免误伤。
     */
    public static String stripDotEllipsisAfterColonsOutsideStrings(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        StringBuilder out = new StringBuilder(s.length());
        boolean inDq = false;
        boolean esc = false;
        int i = 0;
        final int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (esc) {
                out.append(c);
                esc = false;
                i++;
                continue;
            }
            if (inDq) {
                if (c == '\\') {
                    esc = true;
                } else if (c == '"') {
                    inDq = false;
                }
                out.append(c);
                i++;
                continue;
            }
            if (c == '"') {
                inDq = true;
                out.append(c);
                i++;
                continue;
            }
            if (c == ':') {
                out.append(':');
                i++;
                int j = i;
                while (j < n && isJsonWs(s.charAt(j))) {
                    j++;
                }
                boolean hadDots = false;
                while (j < n) {
                    int k = j;
                    while (k < n && (s.charAt(k) == '.' || s.charAt(k) == '\u2026')) {
                        k++;
                        hadDots = true;
                    }
                    if (k == j) {
                        break;
                    }
                    j = k;
                    while (j < n && isJsonWs(s.charAt(j))) {
                        j++;
                    }
                    if (j < n && (s.charAt(j) == '.' || s.charAt(j) == '\u2026')) {
                        continue;
                    }
                    break;
                }
                if (hadDots && j < n) {
                    char peek = s.charAt(j);
                    if (peek == '[' || peek == '{' || peek == '"') {
                        i = j;
                        continue;
                    }
                }
                continue;
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static boolean isJsonWs(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    /**
     * 去掉串外的连续 {@code .}（模型常用 {@code ...} 表示省略，会破坏 JSON）。
     */
    public static String stripDotRunsOutsideDoubleQuotedStrings(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        StringBuilder out = new StringBuilder(s.length());
        boolean inDq = false;
        boolean esc = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (esc) {
                out.append(c);
                esc = false;
                continue;
            }
            if (inDq) {
                if (c == '\\') {
                    esc = true;
                } else if (c == '"') {
                    inDq = false;
                }
                out.append(c);
                continue;
            }
            if (c == '"') {
                inDq = true;
                out.append(c);
                continue;
            }
            if (c == '.') {
                int j = i;
                while (j < s.length() && s.charAt(j) == '.') {
                    j++;
                }
                if (j - i >= 3) {
                    i = j - 1;
                    continue;
                }
            }
            out.append(c);
        }
        return collapseDuplicateCommas(out.toString());
    }

    /** 根对象或数组开头处的空白与句点垃圾（截断/思考残留）。 */
    public static String trimGarbageAfterStructuralOpen(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        t = trimAfterOpenChar(t, '{');
        if (t.startsWith("[")) {
            int i = 1;
            while (i < t.length()) {
                char c = t.charAt(i);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '.') {
                    i++;
                    continue;
                }
                break;
            }
            if (i > 1) {
                t = "[" + t.substring(i);
            }
        }
        return t;
    }

    private static String trimAfterOpenChar(String t, char open) {
        if (t.length() < 2 || t.charAt(0) != open) {
            return t;
        }
        int i = 1;
        while (i < t.length()) {
            char c = t.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '.') {
                i++;
                continue;
            }
            break;
        }
        if (i == 1) {
            return t;
        }
        return open + t.substring(i);
    }

    private static String collapseDuplicateCommas(String s) {
        String t = s;
        for (int n = 0; n < 12; n++) {
            String u = t.replaceAll(",\\s*,", ",");
            if (u.equals(t)) {
                break;
            }
            t = u;
        }
        t = t.replaceAll("\\[\\s*,", "[");
        t = t.replaceAll(",\\s*]", "]");
        t = t.replaceAll("\\{\\s*,", "{");
        return t;
    }
}
