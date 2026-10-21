package com.ohcp.agri.kb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 内存知识库：启动时从 {@code classpath:data/knowledge.json} 载入切片。
 *
 * <p>已实现：JSON 载入、条目计数。
 * <p><b>TODO（你们要写的核心逻辑）</b>：{@link #search} 的相似度计算与排序。
 */
@Component
public class InMemoryKnowledgeBase implements KnowledgeBase {

    private static final Logger log = LoggerFactory.getLogger(InMemoryKnowledgeBase.class);

    private final List<KnowledgeChunk> chunks = new ArrayList<>();

    public InMemoryKnowledgeBase(ObjectMapper objectMapper) {
        load(objectMapper);
    }

    private void load(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource("data/knowledge.json");
        if (!resource.exists()) {
            log.warn("未找到 data/knowledge.json，知识库为空。请先跑爬虫生成知识库。");
            return;
        }
        try (InputStream in = resource.getInputStream()) {
            KnowledgeChunk[] loaded = objectMapper.readValue(in, KnowledgeChunk[].class);
            chunks.addAll(Arrays.asList(loaded));
            log.info("知识库载入完成，共 {} 条切片", chunks.size());
        } catch (Exception e) {
            log.error("知识库载入失败，知识库将为空", e);
        }
    }

    @Override
    public List<ScoredChunk> search(String query, int topK) {
        // TODO: 实现相似度检索并排序取前 topK 条
        // 提示（三种做法任选，答辩时说明取舍）：
        //   1) 关键词/规则召回：症状词 → 元素映射表，最稳、最好解释
        //   2) 向量检索：把 text 向量化后算余弦相似度
        //      - 需要 embedding 接口，或本地 ONNX 模型
        //   3) 混合：关键词召回后再用相似度精排（推荐，既准又能演示"向量化"）
        return List.of();
    }

    @Override
    public int size() {
        return chunks.size();
    }

    /** 暴露只读视图，供测试与调试使用。 */
    public List<KnowledgeChunk> all() {
        return List.copyOf(chunks);
    }
}
