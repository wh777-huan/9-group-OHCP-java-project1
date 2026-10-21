package com.ohcp.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohcp.agent.model.KnowledgeEntry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地私有缺素症知识库：启动时加载爬虫产出的 knowledge.json。
 */
@Service
public class KnowledgeBaseService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, KnowledgeEntry> entriesById = new LinkedHashMap<>();
    private List<KnowledgeEntry> entries = new ArrayList<>();

    @Value("${app.knowledge-file:knowledge/knowledge.json}")
    private String knowledgeFile;

    @PostConstruct
    public void init() throws Exception {
        ClassPathResource resource = new ClassPathResource(knowledgeFile);
        try (InputStream in = resource.getInputStream()) {
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(json);
            JsonNode arr = root.get("entries");
            for (JsonNode node : arr) {
                KnowledgeEntry entry = objectMapper.treeToValue(node, KnowledgeEntry.class);
                if (entry.getId() == null || entry.getId().isBlank()) {
                    continue;
                }
                entriesById.put(entry.getId(), entry);
            }
            entries = new ArrayList<>(entriesById.values());
        }
    }

    public List<KnowledgeEntry> all() {
        return entries;
    }

    public KnowledgeEntry byId(String id) {
        return entriesById.get(id);
    }

    public int size() {
        return entries.size();
    }
}
