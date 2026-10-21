package com.ohcp.project1.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 诊断服务：知识库优先检索 → SpringAI 整理建议 → 思政融入。
 * 禁止绕过知识库直接调 LLM 输出答案。
 */
@Service
public class DiagnoseService {

    private final KnowledgeBaseService knowledgeBase;
    private final ChatClient chatClient;
    private final boolean llmEnabled;

    public DiagnoseService(KnowledgeBaseService knowledgeBase,
                           ObjectProvider<ChatClient.Builder> chatClientBuilder,
                           @Value("${app.llm.enabled:false}") boolean llmEnabled) {
        this.knowledgeBase = knowledgeBase;
        ChatClient.Builder builder = chatClientBuilder.getIfAvailable();
        this.chatClient = builder == null ? null : builder.build();
        this.llmEnabled = llmEnabled;
    }

    public DiagnoseResult diagnose(String symptomText) {
        if (symptomText == null || symptomText.isBlank()) {
            throw new IllegalArgumentException("请先描述作物和症状。");
        }

        // Step 1: 知识库优先检索
        List<KnowledgeBaseService.Candidate> candidates = knowledgeBase.search(symptomText);

        if (candidates.isEmpty()) {
            return new DiagnoseResult("未知", 0.0,
                    "未能在本地知识库中找到匹配的缺素症，请补充作物种类、受影响叶片部位和症状变化。",
                    "", "", "", "", List.of(), "local_fallback", List.of());
        }

        // Step 2: 取置信度最高的 2 个候选，送 LLM 整理
        KnowledgeBaseService.Candidate top = candidates.get(0);
        List<String> topNames = candidates.stream()
                .limit(2)
                .map(c -> c.deficiency.element)
                .collect(Collectors.toList());

        String context = buildContext(candidates);
        String prompt = buildPrompt(symptomText, topNames, context);

        String suggestion;
        String mode = "local_fallback";
        try {
            if (!llmEnabled || chatClient == null) {
                throw new IllegalStateException("未启用大模型，使用本地知识库建议");
            }
            suggestion = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            if (suggestion == null || suggestion.isBlank()) {
                throw new IllegalStateException("模型返回内容为空");
            }
            mode = "llm_enhanced";
        } catch (Exception e) {
            // LLM 调用失败时退回到知识库原始数据
            suggestion = fallbackSuggestion(top.deficiency);
        }

        // Step 3: 思政自然融入（末尾顺带，不弹窗）
        String politicGuidance = "科技兴农，青年有为——将 AI 技术应用到农业农村领域，服务乡村振兴与粮食安全。";

        return new DiagnoseResult(
                top.deficiency.element,
                top.confidence,
                "",
                top.deficiency.organicFertilizer,
                top.deficiency.chemicalFertilizer,
                top.deficiency.safetyNote + "\n\n💡 " + politicGuidance,
                top.deficiency.description,
                suggestion,
                mode,
                candidates.stream().limit(3)
                        .map(c -> new CandidateResult(c.deficiency.element, c.confidence))
                        .toList(),
                List.of()
        );
    }

    private String buildContext(List<KnowledgeBaseService.Candidate> candidates) {
        StringBuilder sb = new StringBuilder();
        for (KnowledgeBaseService.Candidate c : candidates) {
            KnowledgeBaseService.Deficiency d = c.deficiency;
            sb.append(String.format("【%s】置信度 %.0f%%\n症状：%s\n有机肥：%s\n化肥：%s\n",
                    d.element, c.confidence * 100,
                    String.join("；", d.symptoms),
                    d.organicFertilizer,
                    d.chemicalFertilizer));
        }
        return sb.toString();
    }

    private String buildPrompt(String symptomText, List<String> candidates, String context) {
        return String.format("""
                用户描述的症状：%s

                本地知识库检索到的可能缺素类型（按置信度排序）：%s

                知识库数据：
                %s

                请基于以上知识库数据，生成科学的施肥建议。
                输出格式包含：
                - 缺素名称
                - 缺素简介
                - 有机肥/生物肥施用方案
                - 化学肥料追施方案
                - 施肥安全与减量提醒

                注意：必须基于上述知识库数据回答，不要凭空编造。
                """, symptomText, String.join("、", candidates), context);
    }

    private String fallbackSuggestion(KnowledgeBaseService.Deficiency d) {
        return String.format("""
                缺素名称：%s
                缺素简介：%s
                有机肥/生物肥施用方案：%s
                化学肥料追施方案：%s
                施肥安全与减量提醒：%s
                """,
                d.element, d.description, d.organicFertilizer, d.chemicalFertilizer, d.safetyNote);
    }

    public record DiagnoseResult(
            String element,
            double confidence,
            String description,
            String organicFertilizer,
            String chemicalFertilizer,
            String safetyWithGuidance,
            String agronomicAdvice,
            String suggestion,
            String mode,
            List<CandidateResult> candidates,
            List<SourceResult> sources
    ) {}

    public record CandidateResult(String element, double confidence) {}

    public record SourceResult(String title, String url) {}
}
