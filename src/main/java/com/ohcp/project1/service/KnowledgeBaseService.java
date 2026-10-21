package com.ohcp.project1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * 知识库加载与检索。
 * 禁止直接调 LLM 输出答案——本服务实现本地知识库优先检索。
 */
@Service
public class KnowledgeBaseService {

    @Value("${app.knowledge.path}")
    private Resource knowledgeResource;

    private final ObjectMapper mapper = new ObjectMapper();
    private List<Deficiency> deficiencies = new ArrayList<>();

    @PostConstruct
    public void init() throws IOException {
        JsonNode root = mapper.readTree(knowledgeResource.getInputStream());
        JsonNode items = root.get("deficiencies");
        for (JsonNode node : items) {
            Deficiency d = new Deficiency();
            d.id = node.path("id").asText();
            d.element = node.path("element").asText();
            d.description = node.path("description").asText();
            d.organicFertilizer = node.path("organic_fertilizer").asText();
            d.chemicalFertilizer = node.path("chemical_fertilizer").asText();
            d.safetyNote = node.path("safety_note").asText();
            List<String> symptoms = new ArrayList<>();
            node.path("symptoms").forEach(s -> symptoms.add(s.asText()));
            d.symptoms = symptoms;
            deficiencies.add(d);
        }
    }

    /**
     * 根据症状文本检索最匹配的缺素症。
     * 使用关键词命中计分，返回按置信度降序排列的候选列表。
     */
    public List<Candidate> search(String symptomText) {
        String lower = symptomText.toLowerCase();
        Set<String> keywords = new HashSet<>(Arrays.asList(lower.split("[\\s,，、。；;.!！?？]+")));

        List<Candidate> scores = new ArrayList<>();
        for (Deficiency d : deficiencies) {
            int hits = 0;
            for (String kw : keywords) {
                if (kw.length() < 2) continue;
                for (String s : d.symptoms) {
                    if (s.toLowerCase().contains(kw)) hits++;
                }
                if (d.description.toLowerCase().contains(kw)) hits++;
                if (d.element.toLowerCase().contains(kw)) hits++;
            }
            if (hits > 0) {
                double confidence = Math.min(0.99, 0.4 + hits * 0.15);
                scores.add(new Candidate(d, confidence));
            }
        }
        scores.sort((a, b) -> Double.compare(b.confidence, a.confidence));
        return scores;
    }

    public static class Deficiency {
        public String id;
        public String element;
        public List<String> symptoms;
        public String description;
        public String organicFertilizer;
        public String chemicalFertilizer;
        public String safetyNote;
    }

    public static class Candidate {
        public Deficiency deficiency;
        public double confidence;

        public Candidate(Deficiency deficiency, double confidence) {
            this.deficiency = deficiency;
            this.confidence = confidence;
        }
    }
}
