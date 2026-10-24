package com.ohcp.agent.service;

import com.ohcp.agent.model.Candidate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 检索层验收：验收句、口语短句、无关输入必须全部表现正确。 */
class VectorRetrievalTest {

    private static VectorRetrieval retrieval;

    @BeforeAll
    static void setup() throws Exception {
        KnowledgeBaseService kb = new KnowledgeBaseService();
        ReflectionTestUtils.setField(kb, "knowledgeFile", "knowledge/knowledge.json");
        kb.init();
        retrieval = new VectorRetrieval();
        ReflectionTestUtils.setField(retrieval, "confidenceFloor", 0.08);
        retrieval.buildIndex(kb.all());
    }

    @Test
    @DisplayName("验收句：水稻缺氮 → 氮居首且匹配度在 0.5~0.95")
    void riceNitrogen() {
        List<Candidate> hits = retrieval.search("水稻叶片从下部老叶开始均匀发黄，植株矮小，分蘖减少", 3);
        assertFalse(hits.isEmpty());
        assertEquals("氮", hits.get(0).getElement());
        assertTrue(hits.get(0).getConfidence() > 0.5 && hits.get(0).getConfidence() <= 0.95,
                "匹配度应在 0.5~0.95，实际 " + hits.get(0).getConfidence());
    }

    @Test
    @DisplayName("验收句：小麦叶缘焦枯易倒伏 → 钾居首")
    void wheatPotassium() {
        List<Candidate> hits = retrieval.search("小麦叶缘焦枯，叶片边缘发黄干枯，植株易倒伏", 3);
        assertEquals("钾", hits.get(0).getElement());
    }

    @Test
    @DisplayName("验收句：花而不实 → 硼居首")
    void boron() {
        List<Candidate> hits = retrieval.search("开花但不结实，花而不实", 3);
        assertEquals("硼", hits.get(0).getElement());
    }

    @Test
    @DisplayName("口语短句全部命中对应元素")
    void colloquialQueries() {
        assertAll(
                () -> assertEquals("氮", top("老叶发黄")),
                () -> assertEquals("铁", top("新叶发黄")),
                () -> assertEquals("钾", top("叶片边缘干枯")),
                () -> assertEquals("锌", top("小叶簇生")),
                () -> assertEquals("氮", top("缺氮有什么表现")),
                () -> assertEquals("钾", top("水稻倒伏")));
    }

    @Test
    @DisplayName("无关输入不强行归因：返回空候选（confidence-floor 0.08 生效）")
    void irrelevantInputNotAttributed() {
        assertAll(
                () -> assertTrue(retrieval.search("今天天气很好，适合出门散步", 3).isEmpty()),
                () -> assertTrue(retrieval.search("我要去食堂吃饭", 3).isEmpty()),
                () -> assertTrue(retrieval.search("这段代码怎么调试", 3).isEmpty()),
                () -> assertTrue(retrieval.search("你好", 3).isEmpty()),
                () -> assertTrue(retrieval.search(null, 3).isEmpty()),
                () -> assertTrue(retrieval.search("   ", 3).isEmpty()));
    }

    @Test
    @DisplayName("原始余弦分布在 [0,1] 且降序（校准前的真实分数）")
    void rawScoresBoundedAndSorted() {
        List<Candidate> raw = retrieval.searchRaw("水稻叶片从下部老叶开始均匀发黄，植株矮小，分蘖减少", 12);
        assertFalse(raw.isEmpty());
        double prev = Double.MAX_VALUE;
        for (Candidate c : raw) {
            assertTrue(c.getConfidence() >= 0 && c.getConfidence() <= 1, "原始分越界: " + c.getConfidence());
            assertTrue(c.getConfidence() <= prev, "未按降序排列");
            prev = c.getConfidence();
        }
        assertEquals("氮", raw.get(0).getElement());
    }

    private String top(String query) {
        List<Candidate> hits = retrieval.search(query, 3);
        assertFalse(hits.isEmpty(), "「" + query + "」没有检索到候选");
        return hits.get(0).getElement();
    }
}
