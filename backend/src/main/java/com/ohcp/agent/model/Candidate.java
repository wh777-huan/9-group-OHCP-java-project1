package com.ohcp.agent.model;

import java.util.List;

/**
 * 缺素候选结果：多候选输出 + 匹配置信度。
 */
public class Candidate {

    private String id;
    private String element;
    private String name;
    private double confidence;
    private List<String> matchReasons;

    public Candidate() {
    }

    public Candidate(String id, String element, String name, double confidence, List<String> matchReasons) {
        this.id = id;
        this.element = element;
        this.name = name;
        this.confidence = confidence;
        this.matchReasons = matchReasons;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getElement() { return element; }
    public void setElement(String element) { this.element = element; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public List<String> getMatchReasons() { return matchReasons; }
    public void setMatchReasons(List<String> matchReasons) { this.matchReasons = matchReasons; }
}
