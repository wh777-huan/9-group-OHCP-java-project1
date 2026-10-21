package com.ohcp.agri.kb;

import java.util.List;

/**
 * 知识库检索接口。实现可以是内存集合、JSON 文件或向量数据库，
 * 上层代码只依赖这个接口——换存储实现时不用改业务逻辑。
 */
public interface KnowledgeBase {

    /** 按查询串返回最相关的前 topK 条切片，按相关度降序。 */
    List<ScoredChunk> search(String query, int topK);

    /** 当前知识库条目数，用于健康检查与答辩展示。 */
    int size();

    /**
     * 带相关度的检索结果。
     *
     * @param chunk 命中的切片
     * @param score 相关度 0–1，越大越相关
     */
    record ScoredChunk(KnowledgeChunk chunk, double score) {
    }
}
