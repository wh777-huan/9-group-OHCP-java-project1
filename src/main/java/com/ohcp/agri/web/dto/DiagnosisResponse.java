package com.ohcp.agri.web.dto;

import java.util.List;

/**
 * 返回给前端的识别结果。字段覆盖需求文档的「输出内容示例」：
 * 缺素名称、缺素简介、农艺调控、有机/生物肥、化学施肥、安全与减量提醒，
 * 外加思政内容（自然融入，不弹窗）与检索来源。
 */
public record DiagnosisResponse(
        String crop,
        List<Candidate> candidates,
        String introduction,
        String agronomicMeasures,
        String organicFertilizer,
        String chemicalFertilizer,
        String safetyNotice,
        String civicsNote,
        List<String> sources
) {

    /**
     * 单个候选缺素类型。
     *
     * @param element    营养元素，如「氮」「钾」
     * @param confidence 置信度 0–1，来自检索匹配得分
     * @param evidence   命中的知识库依据（便于答辩展示 RAG 确实生效）
     */
    public record Candidate(String element, double confidence, String evidence) {
    }
}
