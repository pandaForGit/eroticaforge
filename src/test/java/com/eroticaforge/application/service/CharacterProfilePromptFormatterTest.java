package com.eroticaforge.application.service;

import com.eroticaforge.infrastructure.persistence.entity.StoryCharacterSnapshotEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CharacterProfilePromptFormatterTest {

    @Test
    void formatsNameAndPersonality() {
        CharacterProfilePromptFormatter fmt = new CharacterProfilePromptFormatter();
        StoryCharacterSnapshotEntity e = new StoryCharacterSnapshotEntity();
        e.setId("s1");
        e.setStoryId("story");
        e.setSortOrder(0);
        e.setPayload(
                Map.of(
                        "name", "测试角色",
                        "personality", "内向",
                        "sample_dialogues", List.of("你好", "再见")));
        e.setCreatedAt(Instant.EPOCH);
        e.setUpdatedAt(Instant.EPOCH);

        String out = fmt.format(List.of(e));
        assertThat(out).contains("#### 测试角色");
        assertThat(out).contains("**性格**");
        assertThat(out).contains("内向");
        assertThat(out).contains("示例台词");
    }

    @Test
    void emptyListYieldsEmptyString() {
        CharacterProfilePromptFormatter fmt = new CharacterProfilePromptFormatter();
        assertThat(fmt.format(List.of())).isEmpty();
        assertThat(fmt.format(null)).isEmpty();
    }
}
