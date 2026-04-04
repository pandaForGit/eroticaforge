package com.eroticaforge.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CorpusTextFilesTest {

    private static final Charset GBK = Charset.forName("GBK");

    @Test
    void readsUtf8(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("a.txt");
        Files.writeString(f, "中文测试abc", StandardCharsets.UTF_8);
        assertThat(CorpusTextFiles.readAllText(f)).contains("中文");
    }

    @Test
    void readsGbkWhenNotUtf8(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("b.txt");
        Files.write(f, "足够长的纯中文".getBytes(GBK));
        assertThat(CorpusTextFiles.readAllText(f)).contains("纯中文");
    }
}
