package com.eroticaforge.dataanalysis.corpus;

import com.eroticaforge.dataanalysis.config.CorpusProcessingProperties;
import com.eroticaforge.dataanalysis.corpus.model.CorpusSanitizeOutcome;
import com.eroticaforge.dataanalysis.corpus.model.CorpusSkipReason;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 语料解码（UTF-8 优先，失败则 GBK）与简单广告行剔除。
 *
 * @author EroticaForge
 */
@Component
public class CorpusSanitizer {

    private static final Charset CHARSET_GBK = Charset.forName("GBK");

    /** 典型推广/外链行（简单规则，可按需扩展）。 */
    private static final Pattern AD_LINE_PATTERN =
            Pattern.compile(
                    "^[\\s　]*("
                            + "https?://.*"
                            + "|www\\.[^\\s]+"
                            + "|.*关注[^\\s]{0,20}微信.*"
                            + "|.*QQ群[:：]?\\s*\\d+.*"
                            + "|.*看.?[更全正]多.*小说.*"
                            + "|.*访问.{0,10}下载.*"
                            + ").*$",
                    Pattern.CASE_INSENSITIVE);

    private final CorpusProcessingProperties properties;

    public CorpusSanitizer(CorpusProcessingProperties properties) {
        this.properties = properties;
    }

    /**
     * 将文件字节解码为字符串并做简单清洗。
     *
     * @param bytes 文件全部字节
     * @return 成功时为 outcome；跳过则 empty
     */
    public Result sanitizeBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return Result.skipped(CorpusSkipReason.EMPTY_AFTER_CLEAN);
        }
        String decoded;
        String charsetLabel;
        try {
            var decoder =
                    StandardCharsets.UTF_8
                            .newDecoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT);
            decoded = decoder.decode(ByteBuffer.wrap(bytes)).toString();
            charsetLabel = "UTF-8";
        } catch (CharacterCodingException ex) {
            decoded = new String(bytes, CHARSET_GBK);
            charsetLabel = "GBK";
        }
        String cleaned = stripObviousAdLines(decoded);
        cleaned = normalizeWhitespace(cleaned);
        if (cleaned.length() < properties.getMinTextChars()) {
            return Result.skipped(CorpusSkipReason.TOO_SHORT);
        }
        return Result.ok(new CorpusSanitizeOutcome(cleaned, charsetLabel));
    }

    private static String stripObviousAdLines(String text) {
        String[] lines = text.split("\\R");
        List<String> kept = new ArrayList<>(lines.length);
        for (String line : lines) {
            if (!AD_LINE_PATTERN.matcher(line).matches()) {
                kept.add(line);
            }
        }
        return String.join("\n", kept);
    }

    private static String normalizeWhitespace(String s) {
        return s.strip().replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
    }

    /**
     * 清洗结果：成功或跳过。
     *
     * @author EroticaForge
     */
    public static final class Result {

        private final CorpusSanitizeOutcome outcome;
        private final CorpusSkipReason skipReason;

        private Result(CorpusSanitizeOutcome outcome, CorpusSkipReason skipReason) {
            this.outcome = outcome;
            this.skipReason = skipReason;
        }

        static Result ok(CorpusSanitizeOutcome outcome) {
            return new Result(outcome, null);
        }

        static Result skipped(CorpusSkipReason reason) {
            return new Result(null, reason);
        }

        public boolean isOk() {
            return outcome != null;
        }

        public CorpusSanitizeOutcome outcome() {
            return outcome;
        }

        public CorpusSkipReason skipReason() {
            return skipReason;
        }
    }
}
