package com.ohcp.agent.service;

import com.ohcp.agent.model.Candidate;
import com.ohcp.agent.model.KnowledgeEntry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 轻量向量检索引擎：中文分词（单字+双字）→ TF-IDF 向量化 → 余弦相似度。
 * 满足"数据切片、向量化、相似度检索匹配"，纯本地实现、零外部依赖，现场演示稳定。
 */
@Service
public class VectorRetrieval {

    /** 每个条目：特征词 -> TF-IDF 权重 */
    private final Map<String, Map<String, Double>> docVectors = new LinkedHashMap<>();
    private final Map<String, List<String>> keywordBank = new LinkedHashMap<>();
    private final Map<String, KnowledgeEntry> idToEntry = new LinkedHashMap<>();

    private static final Pattern CJK = Pattern.compile("[\\u4e00-\\u9fff]");
    private static final Pattern LATIN = Pattern.compile("[a-zA-Z0-9]+");

    /** 由 KnowledgeBaseService 在启动后注入全部条目并建索引 */
    public void buildIndex(List<KnowledgeEntry> entries) {
        docVectors.clear();
        keywordBank.clear();
        idToEntry.clear();

        // 1. 收集文档与特征词
        Map<String, Map<String, Integer>> tf = new LinkedHashMap<>();
        Map<String, Integer> df = new HashMap<>();

        for (KnowledgeEntry e : entries) {
            idToEntry.put(e.getId(), e);
            String doc = docText(e);
            Map<String, Integer> local = termCount(doc);
            tf.put(e.getId(), local);
            List<String> kws = new ArrayList<>();
            if (e.getSymptoms() != null) {
                for (KnowledgeEntry.Symptom s : e.getSymptoms()) {
                    if (s.getKeywords() != null) {
                        kws.addAll(s.getKeywords());
                    }
                    if (s.getDesc() != null) {
                        kws.add(s.getDesc());
                    }
                }
            }
            keywordBank.put(e.getId(), kws);
            for (String term : local.keySet()) {
                df.merge(term, 1, Integer::sum);
            }
        }

        int n = entries.size();
        // 2. 计算 TF-IDF 权重
        for (Map.Entry<String, Map<String, Integer>> e : tf.entrySet()) {
            Map<String, Double> vec = new HashMap<>();
            int totalTerms = e.getValue().values().stream().mapToInt(Integer::intValue).sum();
            for (Map.Entry<String, Integer> t : e.getValue().entrySet()) {
                double tfw = (double) t.getValue() / Math.max(1, totalTerms);
                double idf = Math.log((n + 1.0) / (df.getOrDefault(t.getKey(), 0) + 1.0)) + 1.0;
                vec.put(t.getKey(), tfw * idf);
            }
            docVectors.put(e.getKey(), vec);
        }
    }

    private String docText(KnowledgeEntry e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getName()).append(' ').append(e.getElement()).append(' ');
        if (e.getCrops() != null) {
            sb.append(String.join(" ", e.getCrops())).append(' ');
        }
        if (e.getSymptoms() != null) {
            for (KnowledgeEntry.Symptom s : e.getSymptoms()) {
                sb.append(s.getPart()).append(' ').append(s.getDesc()).append(' ');
                if (s.getKeywords() != null) {
                    sb.append(String.join(" ", s.getKeywords())).append(' ');
                }
            }
        }
        if (e.getTriggers() != null) {
            sb.append(String.join(" ", e.getTriggers())).append(' ');
        }
        if (e.getDescription() != null) {
            sb.append(e.getDescription()).append(' ');
        }
        return sb.toString();
    }

    /** 中文分词：CJK 单字 + 相邻双字 + 拉丁词 */
    private List<String> tokenize(String text) {
        List<String> terms = new ArrayList<>();
        StringBuilder cjk = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fff') {
                cjk.append(c);
            } else {
                if (cjk.length() > 0) {
                    addCjkTerms(terms, cjk.toString());
                    cjk.setLength(0);
                }
            }
        }
        if (cjk.length() > 0) {
            addCjkTerms(terms, cjk.toString());
        }
        Matcher m = LATIN.matcher(text.toLowerCase());
        while (m.find()) {
            terms.add(m.group());
        }
        return terms;
    }

    private void addCjkTerms(List<String> terms, String s) {
        for (int i = 0; i < s.length(); i++) {
            terms.add(String.valueOf(s.charAt(i)));
            if (i + 1 < s.length()) {
                terms.add(s.substring(i, i + 2));
            }
        }
    }

    private Map<String, Integer> termCount(String text) {
        Map<String, Integer> count = new HashMap<>();
        for (String t : tokenize(text)) {
            count.merge(t, 1, Integer::sum);
        }
        return count;
    }

    /**
     * 相似度检索：返回按得分降序的候选，附匹配依据（命中的症状关键词/描述）。
     */
    public List<Candidate> search(String query, int topN) {
        Map<String, Integer> q = termCount(query);
        double qNorm = norm(q);
        List<Candidate> result = new ArrayList<>();

        for (Map.Entry<String, Map<String, Double>> e : docVectors.entrySet()) {
            String id = e.getKey();
            Map<String, Double> vec = e.getValue();
            double dot = 0;
            for (Map.Entry<String, Integer> t : q.entrySet()) {
                Double w = vec.get(t.getKey());
                if (w != null) {
                    dot += t.getValue() * w;
                }
            }
            double dNorm = normRaw(vec);
            double sim = (qNorm * dNorm == 0) ? 0 : dot / (qNorm * dNorm);
            result.add(new Candidate(id, idToEntry.get(id).getElement(), idToEntry.get(id).getName(),
                    sim, matchReasons(query, id)));
        }

        result.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));
        // 置信度归一化：最高分映射为 0.9~0.99 区间，其余按比例
        double max = result.isEmpty() ? 0 : result.get(0).getConfidence();
        if (max > 0) {
            for (Candidate c : result) {
                c.setConfidence(Math.min(0.99, 0.5 + (c.getConfidence() / max) * 0.49));
            }
        }
        return result.subList(0, Math.min(topN, result.size()));
    }

    private double norm(Map<String, Integer> vec) {
        double sum = 0;
        for (int v : vec.values()) {
            sum += v * v;
        }
        return Math.sqrt(sum);
    }

    private double normRaw(Map<String, Double> vec) {
        double sum = 0;
        for (double v : vec.values()) {
            sum += v * v;
        }
        return Math.sqrt(sum);
    }

    /** 找出查询中命中的症状关键词/描述，作为识别依据 */
    private List<String> matchReasons(String query, String id) {
        List<String> reasons = new ArrayList<>();
        List<String> bank = keywordBank.getOrDefault(id, List.of());
        for (String kw : bank) {
            if (kw.length() >= 2 && query.contains(kw) && reasons.size() < 4) {
                reasons.add(kw);
            }
        }
        return reasons;
    }
}
