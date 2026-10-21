package com.ohcp.agri.agent;

import com.ohcp.agri.kb.KnowledgeBase;
import com.ohcp.agri.web.dto.DiagnosisRequest;
import com.ohcp.agri.web.dto.DiagnosisResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 智能体编排层：检索本地知识库 → 把检索结果塞进提示词 → 让大模型整理输出。
 *
 * <p>题目硬性要求：<b>不允许直接调大模型出答案</b>。
 * 所以顺序必须是「先检索、后生成」，并且提示词里要明确「只依据给定资料回答」。
 *
 * <p><b>TODO（你们要写的核心逻辑）</b>：
 * <ol>
 *   <li>调用 {@link KnowledgeBase#search} 拿到候选切片</li>
 *   <li>把切片按相关度聚合成候选缺素类型 + 置信度</li>
 *   <li>拼提示词：系统角色 + 检索到的资料 + 用户症状描述</li>
 *   <li>调 {@link ChatClient} 生成施肥建议</li>
 *   <li>在结果里自然融入思政内容（1–2 行，随业务输出，不要弹窗）</li>
 * </ol>
 */
@Service
public class DiagnosisAgent {

    private static final Logger log = LoggerFactory.getLogger(DiagnosisAgent.class);

    private final ChatClient chatClient;
    private final KnowledgeBase knowledgeBase;

    public DiagnosisAgent(ChatClient.Builder chatClientBuilder, KnowledgeBase knowledgeBase) {
        this.chatClient = chatClientBuilder.build();
        this.knowledgeBase = knowledgeBase;
    }

    public DiagnosisResponse diagnose(DiagnosisRequest request) {
        String query = request.toQuery();
        log.debug("收到症状描述: {}", query);

        List<KnowledgeBase.ScoredChunk> hits = knowledgeBase.search(query, 3);
        log.debug("知识库命中 {} 条", hits.size());

        // TODO: 用 hits 组装候选缺素类型与施肥建议；下面返回的是占位结果
        return new DiagnosisResponse(
                request.crop(),
                List.of(),
                "（尚未实现：请先完成知识库检索与候选聚合）",
                "",
                "",
                "",
                "",
                "",
                hits.stream().map(h -> h.chunk().sourceUrl()).toList()
        );
    }
}
