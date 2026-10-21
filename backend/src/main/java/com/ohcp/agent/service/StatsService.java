package com.ohcp.agent.service;

import com.ohcp.agent.model.StatsResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 日志统计：记录高频查询的缺素类型，模拟农技大数据统计看板（工程创新点）。
 */
@Service
public class StatsService {

    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final AtomicLong total = new AtomicLong();

    public void record(String element) {
        if (element == null || element.isBlank()) {
            return;
        }
        counters.computeIfAbsent(element, k -> new AtomicLong()).incrementAndGet();
        total.incrementAndGet();
    }

    public StatsResult snapshot(int topN) {
        List<Map.Entry<String, AtomicLong>> list = new ArrayList<>(counters.entrySet());
        list.sort(Map.Entry.<String, AtomicLong>comparingByValue(
                Comparator.comparingLong(AtomicLong::get)).reversed());
        List<Object[]> top = new ArrayList<>();
        for (Map.Entry<String, AtomicLong> e : list.subList(0, Math.min(topN, list.size()))) {
            top.add(new Object[]{e.getKey(), e.getValue().get()});
        }
        return new StatsResult(total.get(), top);
    }
}
