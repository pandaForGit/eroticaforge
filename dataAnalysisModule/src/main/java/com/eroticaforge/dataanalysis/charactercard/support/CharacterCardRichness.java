package com.eroticaforge.dataanalysis.charactercard.support;

import com.eroticaforge.dataanalysis.charactercard.model.CharacterCardTrigger;
import com.eroticaforge.dataanalysis.charactercard.model.ExtractedCharacterCard;

import java.util.ArrayList;
import java.util.List;

/**
 * 判断人物卡是否仅为「壳」（例如只有 name、其余全空），用于过滤无效抽取结果。
 *
 * @author EroticaForge
 */
public final class CharacterCardRichness {

    private CharacterCardRichness() {}

    /**
     * 足够写入下游使用的角色：不能只有名字。
     * <ul>
     *   <li>核心叙述字段（身份/外貌/性格/背景/NSFW/心理）至少填 2 项；或
     *   <li>有名字且至少填 1 项核心字段，并配有非空台词或触发项；或
     *   <li>无名字但核心字段至少 3 项（少见，避免误杀纯描述块）。
     * </ul>
     */
    public static boolean isRichEnough(ExtractedCharacterCard c) {
        if (c == null) {
            return false;
        }
        int core = countCoreNarrativeFields(c);
        boolean hasName = !c.getName().isBlank();
        boolean hasDialogue = hasNonBlankDialogue(c);
        boolean hasTrigger = hasNonBlankTrigger(c);
        if (core >= 2) {
            return true;
        }
        if (hasName && core >= 1 && (hasDialogue || hasTrigger)) {
            return true;
        }
        if (core >= 3) {
            return true;
        }
        return false;
    }

    public static boolean anyRichCard(List<ExtractedCharacterCard> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (ExtractedCharacterCard c : list) {
            if (isRichEnough(c)) {
                return true;
            }
        }
        return false;
    }

    public static List<ExtractedCharacterCard> filterRichCards(List<ExtractedCharacterCard> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<ExtractedCharacterCard> out = new ArrayList<>();
        for (ExtractedCharacterCard c : list) {
            if (isRichEnough(c)) {
                out.add(c);
            }
        }
        return out;
    }

    private static int countCoreNarrativeFields(ExtractedCharacterCard c) {
        int n = 0;
        if (!c.getIdentity().isBlank()) {
            n++;
        }
        if (!c.getAppearance().isBlank()) {
            n++;
        }
        if (!c.getPersonality().isBlank()) {
            n++;
        }
        if (!c.getBackground().isBlank()) {
            n++;
        }
        if (!c.getNsfwProfile().isBlank()) {
            n++;
        }
        if (!c.getPsychologyAndRelations().isBlank()) {
            n++;
        }
        return n;
    }

    private static boolean hasNonBlankDialogue(ExtractedCharacterCard c) {
        for (String s : c.getSampleDialogues()) {
            if (s != null && !s.isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNonBlankTrigger(ExtractedCharacterCard c) {
        for (CharacterCardTrigger t : c.getTriggers()) {
            if (t == null) {
                continue;
            }
            if (!t.getKeyword().isBlank() || !t.getReaction().isBlank()) {
                return true;
            }
        }
        return false;
    }
}
