package com.eroticaforge.dataanalysis.charactercard.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型返回的根对象：{@code {"characters":[...]}}。
 *
 * @author EroticaForge
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CharacterCardLlmEnvelope {

    private List<ExtractedCharacterCard> characters = new ArrayList<>();

    public List<ExtractedCharacterCard> getCharacters() {
        return characters;
    }

    public void setCharacters(List<ExtractedCharacterCard> characters) {
        this.characters = characters == null ? new ArrayList<>() : characters;
    }
}
