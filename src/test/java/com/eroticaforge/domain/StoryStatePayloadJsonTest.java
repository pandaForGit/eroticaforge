package com.eroticaforge.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link StoryState.Payload} 与 JSON snake_case 的互转测试。
 *
 * @author EroticaForge
 */
class StoryStatePayloadJsonTest {

    /** 测试用 Jackson 对象映射器。 */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 校验序列化键名为 snake_case，且可反序列化还原。
     *
     * <p>无入参。
     *
     * @throws Exception JSON 序列化或反序列化失败时抛出
     */
    @Test
    void payloadUsesSnakeCaseKeys() throws Exception {
        StoryState.Payload payload = new StoryState.Payload();
        payload.setCurrentSummary("概要");
        payload.getCharacterStates().put("艾拉", "冷静");
        payload.getImportantFacts().add("钥匙在抽屉");
        payload.getWorldFlags().add("night");
        payload.setLastChapterEnding("她关上了门。");

        String serializedJson = mapper.writeValueAsString(payload);
        assertTrue(serializedJson.contains("current_summary"));
        assertTrue(serializedJson.contains("character_states"));
        assertTrue(serializedJson.contains("important_facts"));
        assertTrue(serializedJson.contains("world_flags"));
        assertTrue(serializedJson.contains("last_chapter_ending"));

        StoryState.Payload roundTrip = mapper.readValue(serializedJson, StoryState.Payload.class);
        assertEquals("概要", roundTrip.getCurrentSummary());
        assertEquals("冷静", roundTrip.getCharacterStates().get("艾拉"));
        assertEquals(List.of("钥匙在抽屉"), roundTrip.getImportantFacts());
        assertEquals(List.of("night"), roundTrip.getWorldFlags());
        assertEquals("她关上了门。", roundTrip.getLastChapterEnding());
    }

    /**
     * 校验 {@link StoryState#fromRow} 合并表列与 payload。
     *
     * <p>无入参。
     */
    @Test
    void fromRowMergesPayloadAndColumns() {
        Instant updatedAt = Instant.parse("2025-01-01T00:00:00Z");
        StoryState.Payload payload = new StoryState.Payload();
        payload.setCurrentSummary("x");
        StoryState merged = StoryState.fromRow("sid-1", 3, updatedAt, payload);
        assertEquals("sid-1", merged.getStoryId());
        assertEquals(3, merged.getVersion());
        assertEquals(updatedAt, merged.getUpdatedAt());
        assertEquals("x", merged.getCurrentSummary());
    }
}
