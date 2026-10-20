package com.ohcp.project1;

import com.ohcp.project1.model.Item;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 冒烟测试：整条链路（入口 → 业务 → 仓储 → 输出）跑通即通过。 */
class AppTest {

    @Test
    @DisplayName("run() 端到端返回 3 条种子数据")
    void runEndToEnd() throws Exception {
        List<Item> items = App.run();

        assertEquals(3, items.size());
        assertEquals(2246.50, items.stream().mapToDouble(Item::total).sum(), 1e-9);
    }
}
