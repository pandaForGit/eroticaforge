package com.eroticaforge.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eroticaforge.infrastructure.persistence.mybatis.PostgresJsonbJacksonTypeHandler;

import java.time.Instant;
import java.util.Map;

/**
 * 表 {@code erotica_character_library}：人物卡库（导入母卡）。
 *
 * @author EroticaForge
 */
@TableName(value = "erotica_character_library", autoResultMap = true)
public class CharacterLibraryEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("schema_version")
    private String schemaVersion;

    @TableField("source_relative_path")
    private String sourceRelativePath;

    @TableField("content_sha256")
    private String contentSha256;

    @TableField("role_index")
    private Integer roleIndex;

    @TableField("display_name")
    private String displayName;

    @TableField(typeHandler = PostgresJsonbJacksonTypeHandler.class)
    private Map<String, Object> payload;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public Integer getRoleIndex() {
        return roleIndex;
    }

    public void setRoleIndex(Integer roleIndex) {
        this.roleIndex = roleIndex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
