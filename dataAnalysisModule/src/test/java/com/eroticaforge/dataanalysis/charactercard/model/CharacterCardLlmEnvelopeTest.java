package com.eroticaforge.dataanalysis.charactercard.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CharacterCardLlmEnvelopeTest {

    private final ObjectMapper mapper =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void deserializesCharactersRootAndSnakeCaseFields() throws Exception {
        String json =
                """
                {
                  "extra": "ignored",
                  "characters": [
                    {
                      "name": "甲",
                      "nsfw_profile": "x",
                      "psychology_and_relations": "y",
                      "sample_dialogues": ["a", "b"]
                    }
                  ]
                }
                """;
        CharacterCardLlmEnvelope env = mapper.readValue(json, CharacterCardLlmEnvelope.class);
        assertThat(env.getCharacters()).hasSize(1);
        assertThat(env.getCharacters().get(0).getName()).isEqualTo("甲");
        assertThat(env.getCharacters().get(0).getNsfwProfile()).isEqualTo("x");
        assertThat(env.getCharacters().get(0).getPsychologyAndRelations()).isEqualTo("y");
        assertThat(env.getCharacters().get(0).getSampleDialogues()).containsExactly("a", "b");
    }

    @Test
    void emptyCharactersArray() throws Exception {
        CharacterCardLlmEnvelope env = mapper.readValue("{\"characters\":[]}", CharacterCardLlmEnvelope.class);
        assertThat(env.getCharacters()).isEmpty();
    }
}
