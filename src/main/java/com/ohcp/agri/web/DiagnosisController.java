package com.ohcp.agri.web;

import com.ohcp.agri.agent.DiagnosisAgent;
import com.ohcp.agri.kb.KnowledgeBase;
import com.ohcp.agri.web.dto.DiagnosisRequest;
import com.ohcp.agri.web.dto.DiagnosisResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** REST 接口。前端 index.html 调这两个端点。 */
@RestController
@RequestMapping("/api")
public class DiagnosisController {

    private final DiagnosisAgent agent;
    private final KnowledgeBase knowledgeBase;

    public DiagnosisController(DiagnosisAgent agent, KnowledgeBase knowledgeBase) {
        this.agent = agent;
        this.knowledgeBase = knowledgeBase;
    }

    /** 健康检查 + 知识库条目数，答辩时可用来证明知识库确实加载了。 */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "knowledgeBaseSize", knowledgeBase.size()
        );
    }

    /** 核心接口：症状输入 → 识别 → 施肥建议。 */
    @PostMapping("/diagnose")
    public DiagnosisResponse diagnose(@RequestBody DiagnosisRequest request) {
        return agent.diagnose(request);
    }
}
