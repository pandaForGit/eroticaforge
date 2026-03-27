package com.eroticaforge.dataanalysis.charactercard.support;

import com.eroticaforge.dataanalysis.charactercard.model.CharacterCardTrigger;
import com.eroticaforge.dataanalysis.charactercard.model.ExtractedCharacterCard;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CharacterCardRichnessTest {

    @Test
    void nameOnly_isNotRich() {
        ExtractedCharacterCard c = new ExtractedCharacterCard();
        c.setName("基洛");
        assertThat(CharacterCardRichness.isRichEnough(c)).isFalse();
    }

    @Test
    void namePlusTwoCoreFields_isRich() {
        ExtractedCharacterCard c = new ExtractedCharacterCard();
        c.setName("基洛");
        c.setIdentity("主角");
        c.setAppearance("年轻男性");
        assertThat(CharacterCardRichness.isRichEnough(c)).isTrue();
    }

    @Test
    void namePlusOneCoreAndDialogue_isRich() {
        ExtractedCharacterCard c = new ExtractedCharacterCard();
        c.setName("甲");
        c.setIdentity("学生");
        c.setSampleDialogues(List.of("你好"));
        assertThat(CharacterCardRichness.isRichEnough(c)).isTrue();
    }

    @Test
    void namePlusOneCoreAndTrigger_isRich() {
        ExtractedCharacterCard c = new ExtractedCharacterCard();
        c.setName("甲");
        c.setPersonality("内向");
        CharacterCardTrigger t = new CharacterCardTrigger();
        t.setKeyword("压力");
        t.setReaction("沉默");
        c.setTriggers(List.of(t));
        assertThat(CharacterCardRichness.isRichEnough(c)).isTrue();
    }

    @Test
    void filterRichCards_dropsShell() {
        ExtractedCharacterCard shell = new ExtractedCharacterCard();
        shell.setName("仅名");
        ExtractedCharacterCard rich = new ExtractedCharacterCard();
        rich.setName("乙");
        rich.setBackground("x");
        rich.setAppearance("y");
        List<ExtractedCharacterCard> out =
                CharacterCardRichness.filterRichCards(List.of(shell, rich));
        assertThat(out).containsExactly(rich);
    }
}
