package com.ohcp.project1.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 诊断检索的验收测试。
 *
 * <p>只验证真实功能行为，不做"跑了一个空壳就算通过"。
 * 覆盖：水稻缺氮（需求文档原例）、小麦缺钾、未知描述、空输入边界、匹配度契约。
 */
class KnowledgeBaseServiceTest {

    private static KnowledgeBaseService kb;

    @BeforeAll
    static void load() throws Exception {
        try (InputStream in = KnowledgeBaseServiceTest.class
                .getClassLoader().getResourceAsStream("knowledge/nutrient_deficiency.json")) {
            assertNotNull(in, "知识库 JSON 未找到，检查 resources/knowledge/nutrient_deficiency.json");
            kb = new KnowledgeBaseService(in);
        }
    }

    @Test
    @DisplayName("水稻缺氮：需求文档原例应识别为氮")
    void riceNitrogenDeficiency() {
        List<KnowledgeBaseService.Candidate> hits =
                kb.search("水稻叶片从下部老叶开始均匀发黄，植株矮小，分蘖减少");

        assertFalse(hits.isEmpty(), "应当检索到候选，实际为空");
        String top = hits.get(0).deficiency.element;
        assertTrue(top.contains("氮"), "首位候选应为氮，实际为 " + top);
    }

    @Test
    @DisplayName("小麦缺钾：叶缘焦枯 + 易倒伏应识别为钾")
    void wheatPotassiumDeficiency() {
        List<KnowledgeBaseService.Candidate> hits =
                kb.search("小麦叶缘焦枯，叶片边缘发黄干枯，植株易倒伏");

        assertFalse(hits.isEmpty(), "应当检索到候选，实际为空");
        String top = hits.get(0).deficiency.element;
        assertTrue(top.contains("钾"), "首位候选应为钾，实际为 " + top);
    }

    @Test
    @DisplayName("未知描述：不强行归因，返回空候选")
    void unknownDescriptionReturnsEmpty() {
        List<KnowledgeBaseService.Candidate> hits =
                kb.search("今天天气很好，适合出门散步");

        assertTrue(hits.isEmpty(), "无关描述不应被归因到缺素症");
    }

    @Test
    @DisplayName("空输入：null / 空串 / 全空白均返回空列表且不抛异常")
    void blankInputReturnsEmpty() {
        assertTrue(kb.search(null).isEmpty());
        assertTrue(kb.search("").isEmpty());
        assertTrue(kb.search("   \n\t ").isEmpty());
    }

    @Test
    @DisplayName("匹配度契约：范围 0–1 且按降序排列")
    void scoresAreBoundedAndSorted() {
        List<KnowledgeBaseService.Candidate> hits =
                kb.search("水稻叶片从下部老叶开始均匀发黄，植株矮小，分蘖减少");

        double prev = Double.MAX_VALUE;
        for (KnowledgeBaseService.Candidate c : hits) {
            assertTrue(c.confidence >= 0.0 && c.confidence <= 1.0,
                    "匹配度越界: " + c.confidence);
            assertTrue(c.confidence <= prev, "结果未按匹配度降序");
            prev = c.confidence;
        }
    }

    @Test
    @DisplayName("字段兼容：无 agronomic_advice / sources 时为空值而非 null")
    void missingNewFieldsDegradeGracefully() {
        assertFalse(kb.all().isEmpty());
        for (KnowledgeBaseService.Deficiency d : kb.all()) {
            assertNotNull(d.agronomicAdvice, "agronomicAdvice 不应为 null");
            assertNotNull(d.sources, "sources 不应为 null");
        }
    }

    @Test
    @DisplayName("知识库至少载入 8 类缺素症")
    void knowledgeBaseLoaded() {
        assertTrue(kb.size() >= 8, "知识库条目数不足，实际 " + kb.size());
    }
}
