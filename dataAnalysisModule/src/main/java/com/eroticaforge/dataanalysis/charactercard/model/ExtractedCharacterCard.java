package com.eroticaforge.dataanalysis.charactercard.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * 从小说摘录中抽取的单角色结构化人物卡（与《高质量人物卡》维度对应，字段可为空串）。
 *
 * @author EroticaForge
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtractedCharacterCard {

    private String name = "";

    private String age = "";

    private String identity = "";

    private String appearance = "";

    private String personality = "";

    @JsonProperty("nsfw_profile")
    private String nsfwProfile = "";

    @JsonProperty("psychology_and_relations")
    private String psychologyAndRelations = "";

    private String background = "";

    private List<CharacterCardTrigger> triggers = new ArrayList<>();

    @JsonProperty("sample_dialogues")
    private List<String> sampleDialogues = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age == null ? "" : age;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity == null ? "" : identity;
    }

    public String getAppearance() {
        return appearance;
    }

    public void setAppearance(String appearance) {
        this.appearance = appearance == null ? "" : appearance;
    }

    public String getPersonality() {
        return personality;
    }

    public void setPersonality(String personality) {
        this.personality = personality == null ? "" : personality;
    }

    public String getNsfwProfile() {
        return nsfwProfile;
    }

    public void setNsfwProfile(String nsfwProfile) {
        this.nsfwProfile = nsfwProfile == null ? "" : nsfwProfile;
    }

    public String getPsychologyAndRelations() {
        return psychologyAndRelations;
    }

    public void setPsychologyAndRelations(String psychologyAndRelations) {
        this.psychologyAndRelations = psychologyAndRelations == null ? "" : psychologyAndRelations;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background == null ? "" : background;
    }

    public List<CharacterCardTrigger> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<CharacterCardTrigger> triggers) {
        this.triggers = triggers == null ? new ArrayList<>() : triggers;
    }

    public List<String> getSampleDialogues() {
        return sampleDialogues;
    }

    public void setSampleDialogues(List<String> sampleDialogues) {
        this.sampleDialogues = sampleDialogues == null ? new ArrayList<>() : sampleDialogues;
    }
}
