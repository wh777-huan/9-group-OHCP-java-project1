package com.ohcp.project1.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemTest {

    @Test
    @DisplayName("total() = 数量 × 单价")
    void totalMultipliesQuantityByPrice() {
        Item item = new Item("键盘", 3, 99.5);

        assertEquals(298.5, item.total(), 1e-9);
    }

    @Test
    @DisplayName("数量为 0 时合计为 0")
    void totalIsZeroWhenQuantityZero() {
        assertEquals(0.0, new Item("赠品", 0, 88.0).total(), 1e-9);
    }
}
