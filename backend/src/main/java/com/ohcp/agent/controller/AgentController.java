package com.ohcp.agent.controller;

import com.ohcp.agent.model.AnalyzeRequest;
import com.ohcp.agent.model.AnalyzeResponse;
import com.ohcp.agent.model.StatsResult;
import com.ohcp.agent.service.AgentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体 HTTP 接口：缺素识别与施肥建议、统计看板、健康检查。
 */
@RestController
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * 核心链路：输入症状描述 → 识别缺素类型 → 输出科学施肥建议
     */
    @PostMapping("/api/analyze")
    public AnalyzeResponse analyze(@RequestBody AnalyzeRequest request) {
        return agentService.analyze(request);
    }

    /**
     * 高频缺素查询统计（工程创新：农技大数据看板）
     */
    @GetMapping("/api/stats")
    public StatsResult stats() {
        return agentService.stats().snapshot(5);
    }

    /**
     * 健康检查与运行状态
     */
    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "ok");
        m.put("service", "crop-nutrient-agent");
        m.put("knowledgeEntries", agentService.knowledgeSize());
        m.put("llmEnabled", agentService.llmEnabled());
        return m;
    }

    /**
     * 知识库缺素元素清单（前端提示用）
     */
    @GetMapping("/api/elements")
    public List<String> elements() {
        return List.of("氮", "磷", "钾", "钙", "镁", "硫", "铁", "锌", "硼", "锰", "钼", "铜");
    }
}
