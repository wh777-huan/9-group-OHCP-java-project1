package com.ohcp.project1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * 知识库加载与检索。
 * 禁止直接调 LLM 输出答案——本服务实现本地知识库优先检索。
 *
 * <p><b>检索算法（如实说明）</b>：中文按 <b>字符二元组（bigram）</b> 切分，
 * 构建 TF-IDF 词袋向量，用余弦相似度算与每条缺素症文本的相似度，
 * 再叠加症状关键词命中率作为加权项。
 *
 * <p>这是<b>词面（lexical）相似度</b>，不是语义向量检索，也不是校准过的诊断概率。
 * 对外字段 {@code confidence} 为兼容既有接口保留，实际含义为「匹配度」。
 */
@Service
public class KnowledgeBaseService {

    /** 中文二元组长度 */
    private static final int NGRAM = 2;
    /** 低于此匹配度不返回，避免无关描述被强行归因 */
    private static final double MIN_SCORE = 0.15;
    /** 关键词命中项权重 */
    private static final double KEYWORD_WEIGHT = 0.3;

    @Value("${app.knowledge.path}")
    private Resource knowledgeResource;

    private final ObjectMapper mapper = new ObjectMapper();

    private List<Deficiency> deficiencies = new ArrayList<>();
    /** 每条缺素症正文的 TF-IDF 向量 */
    private List<Map<String, Double>> docVectors = new ArrayList<>();
    /** 词 -> 逆文档频率 */
    private Map<String, Double> idf = Map.of();

    public KnowledgeBaseService() {
    }

    /** 供测试直接注入 JSON 流，绕过 Spring 资源注入。 */
    KnowledgeBaseService(InputStream json) throws IOException {
        loadFrom(json);
    }

    @PostConstruct
    public void init() throws IOException {
        try (InputStream in = knowledgeResource.getInputStream()) {
            loadFrom(in);
        }
    }

    private void loadFrom(InputStream in) throws IOException {
        JsonNode root = mapper.readTree(in);
        JsonNode items = root.path("deficiencies");

        List<Deficiency> parsed = new ArrayList<>();
        for (JsonNode node : items) {
            Deficiency d = new Deficiency();
            d.id = node.path("id").asText();
            d.element = node.path("element").asText();
            d.description = node.path("description").asText();
            d.organicFertilizer = node.path("organic_fertilizer").asText();
            d.chemicalFertilizer = node.path("chemical_fertilizer").asText();
            d.safetyNote = node.path("safety_note").asText();
            // 采集负责成员新增字段；缺失时保持为空，不影响既有数据兼容
            d.agronomicAdvice = node.path("agronomic_advice").asText("");
            d.sources = new ArrayList<>();
            for (JsonNode s : node.path("sources")) {
                String title = s.path("title").asText("");
                String url = s.path("url").asText("");
                if (!title.isBlank() || !url.isBlank()) {
                    d.sources.add(new Source(title, url));
                }
            }
            List<String> symptoms = new ArrayList<>();
            node.path("symptoms").forEach(s -> symptoms.add(s.asText()));
            d.symptoms = symptoms;
            parsed.add(d);
        }

        this.deficiencies = parsed;
        buildIndex();
    }

    /** 预计算 IDF 与每条文档的向量。 */
    private void buildIndex() {
        List<Map<String, Integer>> docTerms = new ArrayList<>();
        Map<String, Integer> df = new HashMap<>();

        for (Deficiency d : deficiencies) {
            Map<String, Integer> tf = termFrequency(documentText(d));
            docTerms.add(tf);
            for (String term : tf.keySet()) {
                df.merge(term, 1, Integer::sum);
            }
        }

        int n = Math.max(1, deficiencies.size());
        Map<String, Double> idfMap = new HashMap<>();
        for (Map.Entry<String, Integer> e : df.entrySet()) {
            idfMap.put(e.getKey(), Math.log((1.0 + n) / (1.0 + e.getValue())) + 1.0);
        }
        this.idf = idfMap;

        List<Map<String, Double>> vectors = new ArrayList<>();
        for (Map<String, Integer> tf : docTerms) {
            vectors.add(toVector(tf));
        }
        this.docVectors = vectors;
    }

    /** 文档正文 = 症状词 + 简介，二者都参与匹配。 */
    private String documentText(Deficiency d) {
        return String.join(" ", d.symptoms) + " " + d.description + " " + d.element;
    }

    /**
     * 根据症状文本检索最匹配的缺素症，按匹配度降序返回。
     *
     * @param symptomText 用户描述；空白输入返回空列表，不抛异常
     */
    public List<Candidate> search(String symptomText) {
        if (symptomText == null || symptomText.isBlank()) {
            return List.of();
        }

        String normalized = normalize(symptomText);
        if (normalized.isEmpty()) {
            return List.of();
        }

        Map<String, Double> queryVector = toVector(termFrequency(normalized));
        if (queryVector.isEmpty()) {
            return List.of();
        }

        List<Candidate> results = new ArrayList<>();
        for (int i = 0; i < deficiencies.size(); i++) {
            Deficiency d = deficiencies.get(i);
            double cosine = cosine(queryVector, docVectors.get(i));
            double keyword = keywordOverlap(normalized, d);
            double score = 0.7 * cosine + KEYWORD_WEIGHT * keyword;

            if (score >= MIN_SCORE) {
                results.add(new Candidate(d, Math.min(0.99, score)));
            }
        }
        results.sort((a, b) -> Double.compare(b.confidence, a.confidence));
        return results;
    }

    /** 症状短语命中率：命中的症状数 / 症状总数，命中越长权重越高。 */
    private double keywordOverlap(String normalizedQuery, Deficiency d) {
        if (d.symptoms.isEmpty()) {
            return 0.0;
        }
        double hit = 0;
        for (String symptom : d.symptoms) {
            String s = normalize(symptom);
            if (s.isEmpty()) {
                continue;
            }
            if (normalizedQuery.contains(s)) {
                hit += 1.0;
            } else {
                // 部分命中：症状短语的二元组有多少出现在查询里
                Set<String> grams = grams(s);
                if (grams.isEmpty()) {
                    continue;
                }
                long matched = grams.stream().filter(normalizedQuery::contains).count();
                hit += (double) matched / grams.size() * 0.6;
            }
        }
        return Math.min(1.0, hit / d.symptoms.size());
    }

    /** 归一化：转小写、去空白与标点，只保留汉字/字母/数字。 */
    private String normalize(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toLowerCase().toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** 词频统计：中文按 bigram，连续字母数字按整词。 */
    private Map<String, Integer> termFrequency(String text) {
        Map<String, Integer> tf = new HashMap<>();
        StringBuilder latin = new StringBuilder();

        for (char c : text.toCharArray()) {
            if (c < 0x4E00 || c > 0x9FFF) {   // 非汉字
                if (Character.isLetterOrDigit(c)) {
                    latin.append(c);
                    continue;
                }
            } else {
                if (latin.length() > 0) {
                    addTerm(tf, latin.toString());
                    latin.setLength(0);
                }
            }
            if (latin.length() > 0) {
                addTerm(tf, latin.toString());
                latin.setLength(0);
            }
        }
        if (latin.length() > 0) {
            addTerm(tf, latin.toString());
        }

        for (String gram : grams(text)) {
            addTerm(tf, gram);
        }
        return tf;
    }

    private Set<String> grams(String text) {
        char[] chars = text.toCharArray();
        Set<String> out = new HashSet<>();
        for (int i = 0; i + NGRAM <= chars.length; i++) {
            boolean bothChinese = chars[i] >= 0x4E00 && chars[i] <= 0x9FFF
                    && chars[i + 1] >= 0x4E00 && chars[i + 1] <= 0x9FFF;
            if (bothChinese) {
                out.add(new String(chars, i, NGRAM));
            }
        }
        return out;
    }

    private void addTerm(Map<String, Integer> tf, String term) {
        if (term.length() >= 2) {
            tf.merge(term, 1, Integer::sum);
        }
    }

    /** TF × IDF，并做 L2 归一化，使余弦相似度不受文本长度影响。 */
    private Map<String, Double> toVector(Map<String, Integer> tf) {
        double defaultIdf = Math.log(2.0) + 1.0;
        Map<String, Double> vec = new HashMap<>();
        double norm = 0;

        for (Map.Entry<String, Integer> e : tf.entrySet()) {
            double weight = (1.0 + Math.log(e.getValue())) * idf.getOrDefault(e.getKey(), defaultIdf);
            vec.put(e.getKey(), weight);
            norm += weight * weight;
        }
        if (norm > 0) {
            double len = Math.sqrt(norm);
            vec.replaceAll((k, v) -> v / len);
        }
        return vec;
    }

    private double cosine(Map<String, Double> a, Map<String, Double> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Map<String, Double> small = a.size() <= b.size() ? a : b;
        Map<String, Double> large = small == a ? b : a;

        double dot = 0;
        for (Map.Entry<String, Double> e : small.entrySet()) {
            Double other = large.get(e.getKey());
            if (other != null) {
                dot += e.getValue() * other;
            }
        }
        return Math.max(0.0, Math.min(1.0, dot));
    }

    /** 知识库条目数，供健康检查与答辩展示。 */
    public int size() {
        return deficiencies.size();
    }

    public List<Deficiency> all() {
        return List.copyOf(deficiencies);
    }

    /** 来源链接，供答辩证明数据来自自建采集而非模型编造。 */
    public record Source(String title, String url) {
    }

    public static class Deficiency {
        public String id;
        public String element;
        public List<String> symptoms;
        public String description;
        public String organicFertilizer;
        public String chemicalFertilizer;
        public String safetyNote;
        /** 农艺调控建议（由采集成员新增，缺失时为空串） */
        public String agronomicAdvice = "";
        /** 资料来源（由采集成员新增，缺失时为空列表） */
        public List<Source> sources = new ArrayList<>();
    }

    public static class Candidate {
        public Deficiency deficiency;
        /**
         * 匹配度 0–1。字段名为兼容既有接口保留，
         * <b>不是</b>校准过的诊断概率，答辩时应说明为「匹配度」。
         */
        public double confidence;

        public Candidate(Deficiency deficiency, double confidence) {
            this.deficiency = deficiency;
            this.confidence = confidence;
        }
    }
}
