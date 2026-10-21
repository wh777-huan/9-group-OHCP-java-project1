package com.ohcp.agent.model;

import java.util.List;

/**
 * 高频缺素查询统计结果（工程创新：日志统计，模拟农技大数据看板）。
 */
public class StatsResult {

    private long total;
    private List<Object[]> top;

    public StatsResult(long total, List<Object[]> top) {
        this.total = total;
        this.top = top;
    }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public List<Object[]> getTop() { return top; }
    public void setTop(List<Object[]> top) { this.top = top; }
}
