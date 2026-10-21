package com.ohcp.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohcp.agent.config.LlmProperties;
import com.ohcp.agent.model.AdviceDto;
import com.ohcp.agent.model.AnalyzeRequest;
import com.ohcp.agent.model.AnalyzeResponse;
import com.ohcp.agent.model.Candidate;
import com.ohcp.agent.model.KnowledgeEntry;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体核心编排：本地知识库检索（RAG）→ 大模型整理输出（可选）→ 兜底规则渲染。
 * 业务核心识别逻辑始终依赖本地知识库，大模型仅做结果整理增强，禁止完全依赖大模型直接生成。
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final KnowledgeBaseService kb;
    private final VectorRetrieval retrieval;
    private final StatsService stats;
    private final TemplateRenderer renderer;
    private final LlmProperties llmProps;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.top-candidates:3}")
    private int topCandidates;

    public AgentService(KnowledgeBaseService kb, VectorRetrieval retrieval, StatsService stats,
                        TemplateRenderer renderer, LlmProperties llmProps, ChatModel chatModel) {
        this.kb = kb;
        this.retrieval = retrieval;
        this.stats = stats;
        this.renderer = renderer;
        this.llmProps = llmProps;
        this.chatModel = chatModel;
    }

    @PostConstruct
    public void init() {
        retrieval.buildIndex(kb.all());
        log.info("知识库就绪：{} 个缺素条目，向量索引已构建", kb.size());
    }

    public AnalyzeResponse analyze(AnalyzeRequest req) {
        String input = req.getInput() == null ? "" : req.getInput().trim();
        String mode = "tech".equals(req.getMode()) ? "tech" : "farmer";

        AnalyzeResponse resp = new AnalyzeResponse();
        resp.setMode(mode);

        if (input.isEmpty()) {
            resp.setSuccess(false);
            resp.setMessage("请输入作物长势与缺素症状描述，例如：水稻，叶片从下部老叶开始均匀发黄，植株矮小。");
            resp.setStats(stats.snapshot(5).getTop());
            return resp;
        }

        // 1. 本地知识库检索（RAG 检索增强）
        List<Candidate> candidates = retrieval.search(input, topCandidates);
        if (candidates.isEmpty()) {
            resp.setSuccess(false);
            resp.setMessage("本地知识库暂未匹配到明确的缺素类型，请补充症状部位、表现和生长环境后重试。");
            resp.setStats(stats.snapshot(5).getTop());
            return resp;
        }
        Candidate top = candidates.get(0);
        KnowledgeEntry entry = kb.byId(top.getId());
        if (entry == null) {
            resp.setSuccess(false);
            resp.setMessage("知识库条目缺失，请检查 knowledge.json。");
            return resp;
        }

        // 2. 风险预警（创新点：结合环境输入评估养分缺乏风险）
        String risk = computeRisk(input, top.getConfidence());

        // 3. 统计（创新点：高频查询日志）
        stats.record(top.getElement());

        // 4. 结果生成：优先大模型整理，失败自动降级纯知识库渲染
        AdviceDto advice = null;
        boolean llmUsed = false;
        String source = "本地知识库检索 + 规则渲染（无大模型依赖）";
        if (llmProps.usable() && llmProps.isEnabled()) {
            try {
                advice = llmGenerate(input, candidates, mode);
                llmUsed = true;
                source = "本地知识库检索（RAG）+ 大模型整理输出";
                log.info("LLM 整理成功：{}", top.getElement());
            } catch (Exception e) {
                log.warn("LLM 调用失败，降级规则渲染：{}", e.getMessage());
                advice = null;
            }
        }
        if (advice == null) {
            advice = renderer.render(entry);
            // 简易版由渲染器生成；专业版展示完整字段
        }

        // 5. 组装响应（简易版只保留 simple + 关键字段，专业版输出完整方案）
        if (mode.equals("farmer")) {
            AdviceDto simple = new AdviceDto();
            simple.setElement(advice.getElement());
            simple.setName(advice.getName());
            simple.setDescription(advice.getDescription());
            simple.setSimple(advice.getSimple() != null ? advice.getSimple() : advice.getChemical());
            simple.setScienceCard(advice.getScienceCard());
            simple.setEducation(advice.getEducation());
            simple.setSafety(advice.getSafety());
            advice = simple;
        }

        resp.setSuccess(true);
        resp.setCandidates(candidates);
        resp.setAdvice(advice);
        resp.setRiskLevel(risk);
        resp.setLlmUsed(llmUsed);
        resp.setSource(source);
        resp.setStats(stats.snapshot(5).getTop());
        return resp;
    }

    /** 大模型整理：基于检索到的知识库内容生成结构化建议（RAG 增强） */
    private AdviceDto llmGenerate(String input, List<Candidate> candidates, String mode) throws Exception {
        String context = buildContext(candidates);
        String system = """
                你是农业植保专家智能体，服务于农作物缺素症识别与科学施肥建议。
                规则：
                1. 只能依据用户提供的"本地知识库检索结果"作答，禁止编造知识库中不存在的内容；
                2. 识别可能缺乏的营养元素（可多候选），置信度取 0-1；
                3. 施肥建议必须包含：缺素简介、农艺调控、有机/生物肥施用、化学追施方案、施肥安全与化肥减量增效提醒；
                4. 在 advice.education 字段中自然融入 1-2 句与农业、粮食安全、科技助农、绿色农业相关的简短引导，不生硬说教、不堆砌；
                5. 严格只输出一个 JSON 对象，不要输出任何其他文字或解释。
                """;
        String user = "作物长势与缺素症状描述：" + input + "\n\n本地知识库检索结果（按相似度排序）：\n" + context
                + "\n\n请严格按如下 JSON 结构输出：\n"
                + "{\"candidates\":[{\"id\":\"N\",\"element\":\"氮\",\"name\":\"缺氮症\",\"confidence\":0.87,"
                + "\"matchReasons\":[\"下部老叶均匀发黄\",\"植株矮小\"]}],"
                + "\"advice\":{\"element\":\"氮\",\"name\":\"缺氮症\",\"description\":\"...\",\"agronomic\":\"...\","
                + "\"organic\":\"...\",\"chemical\":\"...\",\"safety\":\"...\",\"scienceCard\":\"...\","
                + "\"education\":\"...\",\"simple\":\"...\"},"
                + "\"riskLevel\":\"低|中|高\"}";

        ChatResponse response = chatModel.chat(ChatRequest.builder()
                .messages(List.of(
                        SystemMessage.from(system),
                        UserMessage.from(user)))
                .build());
        String text = response.aiMessage().text();
        String json = stripFences(text);
        JsonNode root = objectMapper.readTree(json);

        AdviceDto advice = new AdviceDto();
        JsonNode a = root.get("advice");
        if (a != null) {
            advice.setElement(textOr(a, "element"));
            advice.setName(textOr(a, "name"));
            advice.setDescription(textOr(a, "description"));
            advice.setAgronomic(textOr(a, "agronomic"));
            advice.setOrganic(textOr(a, "organic"));
            advice.setChemical(textOr(a, "chemical"));
            advice.setSafety(textOr(a, "safety"));
            advice.setScienceCard(textOr(a, "scienceCard"));
            advice.setEducation(textOr(a, "education"));
            advice.setSimple(textOr(a, "simple"));
        }
        // 字段缺失时用知识库兜底
        Candidate top = candidates.get(0);
        KnowledgeEntry entry = kb.byId(top.getId());
        if (entry != null) {
            fillIfBlank(advice, entry);
        }
        return advice;
    }

    private void fillIfBlank(AdviceDto advice, KnowledgeEntry entry) {
        if (isBlank(advice.getElement())) advice.setElement(entry.getElement());
        if (isBlank(advice.getName())) advice.setName(entry.getName());
        if (isBlank(advice.getDescription())) advice.setDescription(entry.getDescription());
        if (isBlank(advice.getAgronomic())) advice.setAgronomic(entry.getAgronomic());
        if (isBlank(advice.getOrganic())) advice.setOrganic(entry.getOrganic());
        if (isBlank(advice.getChemical())) advice.setChemical(entry.getChemical());
        if (isBlank(advice.getSafety())) advice.setSafety(entry.getSafety());
        if (isBlank(advice.getScienceCard())) advice.setScienceCard(entry.getScienceCard());
        if (isBlank(advice.getEducation())) advice.setEducation(entry.getEducation());
        if (isBlank(advice.getSimple())) {
            advice.setSimple(renderer.render(entry).getSimple());
        }
    }

    private String textOr(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 检索上下文：首候选全量，其余简要，控制 token 消耗 */
    private String buildContext(List<Candidate> candidates) throws Exception {
        List<Map<String, Object>> ctx = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            KnowledgeEntry e = kb.byId(c.getId());
            if (e == null) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("element", e.getElement());
            m.put("name", e.getName());
            m.put("confidence", Math.round(c.getConfidence() * 100) / 100.0);
            if (i == 0) {
                m.put("crops", e.getCrops());
                m.put("symptoms", e.getSymptoms());
                m.put("description", e.getDescription());
                m.put("agronomic", e.getAgronomic());
                m.put("organic", e.getOrganic());
                m.put("chemical", e.getChemical());
                m.put("safety", e.getSafety());
                m.put("scienceCard", e.getScienceCard());
                m.put("education", e.getEducation());
            } else {
                m.put("description", e.getDescription());
            }
            ctx.add(m);
        }
        return objectMapper.writeValueAsString(ctx);
    }

    private String stripFences(String text) {
        String s = text.trim();
        if (s.startsWith("```")) {
            int first = s.indexOf('\n');
            if (first > 0) {
                s = s.substring(first + 1);
            }
            int last = s.lastIndexOf("```");
            if (last >= 0) {
                s = s.substring(0, last);
            }
        }
        return s.trim();
    }

    /** 风险预警：结合土壤类型/生育期/环境输入关键词粗判 */
    private String computeRisk(String input, double confidence) {
        String[] highRisk = {"沙质", "沙壤", "石灰性", "碱性土", "连续降雨", "多雨", "淋溶", "低温", "冷浸",
                "连作", "渍水", "排水不良", "干旱", "暴雨"};
        for (String w : highRisk) {
            if (input.contains(w)) {
                return "高";
            }
        }
        if (confidence >= 0.7) {
            return "中";
        }
        return "低";
    }

    public StatsService stats() {
        return stats;
    }

    public boolean llmEnabled() {
        return llmProps.usable() && llmProps.isEnabled();
    }

    public int knowledgeSize() {
        return kb.size();
    }
}
