package com.ohcp.agri.kb;

/**
 * 知识库切片：爬虫抓取 → 清洗 → 切片后的一条记录。
 *
 * <p>建议先按「作物 + 元素」切片，每片保留原文片段，便于答辩时展示检索依据。
 * 字段可按你们抓到的数据增删，但 {@code text} 必须是用于相似度计算的正文。
 *
 * @param id       唯一标识
 * @param crop     作物（"通用" 表示不限作物）
 * @param element  营养元素（氮/磷/钾/钙/镁/铁/锌/硼…）
 * @param symptom  症状关键词串，冗余字段，便于关键词召回
 * @param text     正文，切片后的原文
 * @param sourceUrl 来源链接，答辩时证明数据来自自建爬虫
 */
public record KnowledgeChunk(
        String id,
        String crop,
        String element,
        String symptom,
        String text,
        String sourceUrl
) {
}
