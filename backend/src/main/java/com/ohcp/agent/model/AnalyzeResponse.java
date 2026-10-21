package com.ohcp.agent.model;

import java.util.List;

/**
 * 分析响应：多候选识别结果 + 施肥建议 + 思政/科普 + 统计。
 */
public class AnalyzeResponse {

    private boolean success;
    private String message;
    private List<Candidate> candidates;
    private AdviceDto advice;
    /** 养分缺乏风险等级：低 / 中 / 高 */
    private String riskLevel;
    /** farmer 农户简易版 / tech 农技专业版 */
    private String mode;
    /** 是否由大模型整理输出（false = 纯知识库规则输出） */
    private boolean llmUsed;
    /** 数据来源说明 */
    private String source;
    /** 高频查询统计 [[元素, 次数], ...] */
    private List<Object[]> stats;
    private long ts = System.currentTimeMillis();

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<Candidate> getCandidates() { return candidates; }
    public void setCandidates(List<Candidate> candidates) { this.candidates = candidates; }
    public AdviceDto getAdvice() { return advice; }
    public void setAdvice(AdviceDto advice) { this.advice = advice; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public boolean isLlmUsed() { return llmUsed; }
    public void setLlmUsed(boolean llmUsed) { this.llmUsed = llmUsed; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public List<Object[]> getStats() { return stats; }
    public void setStats(List<Object[]> stats) { this.stats = stats; }
    public long getTs() { return ts; }
    public void setTs(long ts) { this.ts = ts; }
}
