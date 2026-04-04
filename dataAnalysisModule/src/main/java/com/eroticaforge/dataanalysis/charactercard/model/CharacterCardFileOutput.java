package com.eroticaforge.dataanalysis.charactercard.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 写入人物卡 JSONL 的一行。
 *
 * @author EroticaForge
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CharacterCardFileOutput {

    public static final String SCHEMA_VERSION_VALUE = "1";

    @JsonProperty("schema_version")
    private String schemaVersion = SCHEMA_VERSION_VALUE;

    @JsonProperty("source_relative_path")
    private String sourceRelativePath;

    @JsonProperty("content_sha256")
    private String contentSha256;

    private List<ExtractedCharacterCard> characters;

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getSourceRelativePath() {
        return sourceRelativePath;
    }

    public void setSourceRelativePath(String sourceRelativePath) {
        this.sourceRelativePath = sourceRelativePath;
    }

    public String getContentSha256() {
        return contentSha256;
    }

    public void setContentSha256(String contentSha256) {
        this.contentSha256 = contentSha256;
    }

    public List<ExtractedCharacterCard> getCharacters() {
        return characters;
    }

    public void setCharacters(List<ExtractedCharacterCard> characters) {
        this.characters = characters;
    }

    /**
     * 组装输出行。
     *
     * @param relativePath 相对输入根
     * @param sha256Hex    文件 SHA-256
     * @param characters   抽取结果
     * @return 行 DTO
     */
    public static CharacterCardFileOutput of(
            String relativePath, String sha256Hex, List<ExtractedCharacterCard> characters) {
        CharacterCardFileOutput o = new CharacterCardFileOutput();
        o.setSourceRelativePath(relativePath);
        o.setContentSha256(sha256Hex);
        o.setCharacters(characters);
        return o;
    }
}
