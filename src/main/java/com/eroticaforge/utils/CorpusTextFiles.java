package com.eroticaforge.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 语料 txt 读取：UTF-8 严格解码，失败则按 GBK（与数据分析模块一致）。
 *
 * @author EroticaForge
 */
public final class CorpusTextFiles {

    private static final Charset GBK = Charset.forName("GBK");

    private CorpusTextFiles() {}

    /**
     * 读取整文件为字符串。
     *
     * @param path 文件路径
     * @return 文本
     */
    public static String readAllText(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException ex) {
            return new String(bytes, GBK);
        }
    }
}
