package com.eroticaforge.dataanalysis.corpus;

import com.eroticaforge.dataanalysis.config.CorpusProcessingProperties;
import com.eroticaforge.dataanalysis.corpus.model.CorpusSkipReason;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CorpusSanitizerTest {

    private static final Charset GBK = Charset.forName("GBK");

    @Test
    void utf8BytesDetectedAsUtf8() {
        CorpusProcessingProperties p = new CorpusProcessingProperties();
        p.setMinTextChars(10);
        CorpusSanitizer sanitizer = new CorpusSanitizer(p);
        byte[] utf8 = "足够长的纯中文正文用于单元测试长度门槛".getBytes(StandardCharsets.UTF_8);
        CorpusSanitizer.Result r = sanitizer.sanitizeBytes(utf8);
        assertThat(r.isOk()).isTrue();
        assertThat(r.outcome().detectedCharset()).isEqualTo("UTF-8");
        assertThat(r.outcome().utf16Text()).contains("单元测试");
    }

    @Test
    void gbkBytesFallbackToGbk() {
        CorpusProcessingProperties p = new CorpusProcessingProperties();
        p.setMinTextChars(10);
        CorpusSanitizer sanitizer = new CorpusSanitizer(p);
        byte[] gbk = "足够长的纯中文正文用于单元测试长度门槛".getBytes(GBK);
        CorpusSanitizer.Result r = sanitizer.sanitizeBytes(gbk);
        assertThat(r.isOk()).isTrue();
        assertThat(r.outcome().detectedCharset()).isEqualTo("GBK");
    }

    @Test
    void tooShortSkipped() {
        CorpusProcessingProperties p = new CorpusProcessingProperties();
        p.setMinTextChars(500);
        CorpusSanitizer sanitizer = new CorpusSanitizer(p);
        byte[] utf8 = "短".getBytes(StandardCharsets.UTF_8);
        CorpusSanitizer.Result r = sanitizer.sanitizeBytes(utf8);
        assertThat(r.isOk()).isFalse();
        assertThat(r.skipReason()).isEqualTo(CorpusSkipReason.TOO_SHORT);
    }
}
