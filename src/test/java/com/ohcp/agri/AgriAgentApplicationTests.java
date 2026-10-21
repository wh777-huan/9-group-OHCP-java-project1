package com.ohcp.agri;

import com.ohcp.agri.kb.KnowledgeBase;
import com.ohcp.agri.web.dto.DiagnosisRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 骨架自检：上下文能起来、知识库能注入、接口能响应、请求拼句正确。
 * 这些绿色灯是"环境没问题"的证据；业务用例等你们实现后再补。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AgriAgentApplicationTests {

    @Autowired
    private KnowledgeBase knowledgeBase;

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("Spring 上下文加载成功")
    void contextLoads() {
        assertNotNull(knowledgeBase);
    }

    @Test
    @DisplayName("健康检查返回 ok")
    void healthEndpointWorks() {
        ResponseEntity<Map> resp = rest.getForEntity("/api/health", Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("ok", resp.getBody().get("status"));
        assertTrue(((Number) resp.getBody().get("knowledgeBaseSize")).intValue() >= 0);
    }

    @Test
    @DisplayName("诊断接口可接收请求并返回结构")
    void diagnoseEndpointReturnsShape() {
        DiagnosisRequest req = new DiagnosisRequest("水稻", "下部老叶", "均匀发黄", "沙质土");

        ResponseEntity<Map> resp = rest.postForEntity("/api/diagnose", req, Map.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("水稻", resp.getBody().get("crop"));
        assertNotNull(resp.getBody().get("candidates"));
    }

    @Test
    @DisplayName("症状字段拼成自然语言查询串")
    void requestQueryJoinsFields() {
        DiagnosisRequest req = new DiagnosisRequest("水稻", "下部老叶", "均匀发黄", null);

        assertEquals("水稻，下部老叶，均匀发黄", req.toQuery());
    }
}
