package com.ohcp.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 知识库条目：一种缺素症的完整诊断与施肥数据。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KnowledgeEntry {

    private String id;
    private String element;
    private String name;
    private List<String> crops;
    private List<Symptom> symptoms;
    private List<String> triggers;
    private String description;
    private String agronomic;
    private String organic;
    private String chemical;
    private String safety;
    private String scienceCard;
    private String education;

    public static class Symptom {
        private String part;
        private String desc;
        private List<String> keywords;

        public String getPart() { return part; }
        public void setPart(String part) { this.part = part; }
        public String getDesc() { return desc; }
        public void setDesc(String desc) { this.desc = desc; }
        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getElement() { return element; }
    public void setElement(String element) { this.element = element; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getCrops() { return crops; }
    public void setCrops(List<String> crops) { this.crops = crops; }
    public List<Symptom> getSymptoms() { return symptoms; }
    public void setSymptoms(List<Symptom> symptoms) { this.symptoms = symptoms; }
    public List<String> getTriggers() { return triggers; }
    public void setTriggers(List<String> triggers) { this.triggers = triggers; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAgronomic() { return agronomic; }
    public void setAgronomic(String agronomic) { this.agronomic = agronomic; }
    public String getOrganic() { return organic; }
    public void setOrganic(String organic) { this.organic = organic; }
    public String getChemical() { return chemical; }
    public void setChemical(String chemical) { this.chemical = chemical; }
    public String getSafety() { return safety; }
    public void setSafety(String safety) { this.safety = safety; }
    public String getScienceCard() { return scienceCard; }
    public void setScienceCard(String scienceCard) { this.scienceCard = scienceCard; }
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
}
