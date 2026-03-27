package com.eroticaforge.dataanalysis.corpus;

import com.eroticaforge.dataanalysis.corpus.support.LlmJsonExtractor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmJsonExtractorTest {

    @Test
    void stripsMarkdownFence() {
        String raw =
                """
                Here:
                ```json
                {"a":1,"b":"x"}
                ```
                """;
        assertThat(LlmJsonExtractor.extractJsonObject(raw)).isEqualTo("{\"a\":1,\"b\":\"x\"}");
    }

    @Test
    void plainObject() {
        assertThat(LlmJsonExtractor.extractJsonObject("prefix {\"k\":2} suffix")).isEqualTo("{\"k\":2}");
    }

    @Test
    void emptyThrows() {
        assertThatThrownBy(() -> LlmJsonExtractor.extractJsonObject("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void singleQuotedKeysNestedObject() {
        String raw =
                """
                ```json
                {'characters': [{'name': '甲', 'age': ''}]}
                ```
                """;
        assertThat(LlmJsonExtractor.extractJsonObject(raw))
                .isEqualTo("{'characters': [{'name': '甲', 'age': ''}]}");
    }

    @Test
    void normalizeArrayStripsBacktickAfterBracket() {
        assertThat(LlmJsonExtractor.normalizeArrayAfterOpeningBracket("[ `{ \"a\": 1 }` ]"))
                .isEqualTo("[{ \"a\": 1 }` ]");
    }

    @Test
    void preprocessStripsThinkingPreambleBeforeCharactersJson() {
        String filler = "x".repeat(320);
        String tail = "{\"characters\": [{\"name\": \"甲\", \"age\": \"\"}]}";
        String raw = "Thinking Process:\n1. Analyze the request\n" + filler + "\n" + tail;
        assertThat(LlmJsonExtractor.preprocess(raw)).isEqualTo(tail);
    }

    @Test
    void preprocessIgnoresPlaceholderCharactersInThinkingAndSlicesRealJson() {
        String filler = "x".repeat(320);
        String fake = "So output `{\"characters\": [...]}` only.\n";
        String real = "{\"characters\": [{\"name\": \"甲\", \"age\": \"\"}]}";
        String raw = "Thinking Process:\n" + filler + "\n" + fake + real;
        assertThat(LlmJsonExtractor.preprocess(raw)).isEqualTo(real);
    }

    @Test
    void preprocessDoesNotSliceWhenOnlyPlaceholderNoRealEnvelope() {
        String filler = "x".repeat(320);
        String raw = "Thinking Process:\n" + filler + "\nSee {\"characters\": [...]} for shape.\n";
        String out = LlmJsonExtractor.preprocess(raw);
        assertThat(out).contains("Thinking Process:");
        assertThat(out).contains("[...]");
    }
}
