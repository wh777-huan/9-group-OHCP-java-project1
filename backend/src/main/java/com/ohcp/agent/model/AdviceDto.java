package com.ohcp.agent.model;

/**
 * 科学施肥建议（主诊断对应条目的完整方案）。
 */
public class AdviceDto {

    private String element;
    private String name;
    private String description;
    private String agronomic;
    private String organic;
    private String chemical;
    private String safety;
    private String scienceCard;
    private String education;
    /** 农户简易版：2-3 句口语化要点 */
    private String simple;

    public String getElement() { return element; }
    public void setElement(String element) { this.element = element; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
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
    public String getSimple() { return simple; }
    public void setSimple(String simple) { this.simple = simple; }
}
