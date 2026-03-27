package com.eroticaforge.dataanalysis.charactercard.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CharacterCardJsonRepairTest {

    @Test
    void stripDotEllipsis_singleDotBeforeArray_afterCharacters() {
        String in = "{\"characters\":.[{\"name\":\"a\"}]}";
        assertThat(CharacterCardJsonRepair.stripDotEllipsisAfterColonsOutsideStrings(in))
                .isEqualTo("{\"characters\":[{\"name\":\"a\"}]}");
    }

    @Test
    void stripDotEllipsis_doesNotEatDecimal() {
        String in = "{\"ratio\": 0.5, \"characters\": [{\"name\":\"a\"}]}";
        assertThat(CharacterCardJsonRepair.stripDotEllipsisAfterColonsOutsideStrings(in)).isEqualTo(in);
    }

    @Test
    void applyStandardRepairs_chain() {
        String in = "{\"characters\": ... [{\"name\":\"甲\"}]}";
        String out = CharacterCardJsonRepair.applyStandardRepairs(in);
        assertThat(out).isEqualTo("{\"characters\":[{\"name\":\"甲\"}]}");
    }

    @Test
    void stripDotEllipsis_spacedDotsBeforeArray() {
        String in = "{\"characters\": . . . [{\"name\":\"a\"}]}";
        assertThat(CharacterCardJsonRepair.stripDotEllipsisAfterColonsOutsideStrings(in))
                .isEqualTo("{\"characters\":[{\"name\":\"a\"}]}");
    }
}
