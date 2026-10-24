package com.ohcp.agent.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全链路验收：HTTP → AgentService → 向量检索 → 知识库渲染。
 * llm.enabled=false 走本地规则渲染，结果可重复、零外网依赖。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AnalyzeApiTest {

    @Autowired
    private TestRestTemplate rest;
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode post(String input, String mode) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        HttpEntity<String> entity = new HttpEntity<>(
                mapper.writeValueAsString(Map.of("input", input, "mode", mode)), headers);
        ResponseEntity<String> resp = rest.postForEntity("/api/analyze", entity, String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return mapper.readTree(resp.getBody());
    }

    @Test
    @DisplayName("健康检查：12 条知识库、LLM 关闭")
    void health() throws Exception {
        JsonNode h = mapper.readTree(rest.getForEntity("/api/health", String.class).getBody());
        assertEquals("ok", h.path("status").asText());
        assertEquals(12, h.path("knowledgeEntries").asInt());
        assertFalse(h.path("llmEnabled").asBoolean(), "未配真实 Key 时不得声称启用了大模型");
    }

    @Test
    @DisplayName("农户版：氮识别正确，且施肥方案（有机肥/化肥/农艺）必须完整输出")
    void farmerModeHasFertilizerPlans() throws Exception {
        JsonNode r = post("水稻叶片从下部老叶开始均匀发黄，植株矮小，分蘖减少", "farmer");
        assertTrue(r.path("success").asBoolean());
        assertEquals("氮", r.path("candidates").get(0).path("element").asText());

        JsonNode advice = r.path("advice");
        assertFalse(advice.path("organic").asText("").isBlank(), "农户版缺少有机肥方案");
        assertFalse(advice.path("chemical").asText("").isBlank(), "农户版缺少化肥方案");
        assertFalse(advice.path("agronomic").asText("").isBlank(), "农户版缺少农艺调控");
        assertFalse(advice.path("safety").asText("").isBlank(), "农户版缺少安全提醒");
        assertFalse(advice.path("education").asText("").isBlank(), "农户版缺少思政引导");
    }

    @Test
    @DisplayName("农技版：输出完整方案与科普卡片")
    void techModeComplete() throws Exception {
        JsonNode r = post("小麦叶缘焦枯，叶片边缘发黄干枯，植株易倒伏", "tech");
        assertTrue(r.path("success").asBoolean());
        assertEquals("钾", r.path("candidates").get(0).path("element").asText());
        JsonNode advice = r.path("advice");
        assertFalse(advice.path("organic").asText("").isBlank());
        assertFalse(advice.path("chemical").asText("").isBlank());
        assertFalse(advice.path("scienceCard").asText("").isBlank(), "农技版应有科普卡片");
    }

    @Test
    @DisplayName("无关输入：success=false 并提示补充信息，不强行归因")
    void irrelevantInputNotAttributed() throws Exception {
        JsonNode r = post("今天天气很好，适合出门散步", "farmer");
        assertFalse(r.path("success").asBoolean(), "无关输入不应返回诊断结果");
        assertFalse(r.path("message").asText("").isBlank(), "应给出补充信息的引导");
    }

    @Test
    @DisplayName("空白输入：success=false 带示例提示")
    void blankInput() throws Exception {
        JsonNode r = post("   ", "farmer");
        assertFalse(r.path("success").asBoolean());
        assertTrue(r.path("message").asText().contains("请输入"));
    }

    @Test
    @DisplayName("元素列表：12 种缺素元素")
    void elements() throws Exception {
        JsonNode els = mapper.readTree(rest.getForEntity("/api/elements", String.class).getBody());
        assertEquals(12, els.size());
    }
}
