package com.ohcp.project1.util;

import com.ohcp.project1.model.Item;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTest {

    @Test
    @DisplayName("对象序列化包含字段名与值")
    void toJsonContainsFields() {
        String json = Json.toJson(new Item(1, "键盘", 2, 99.5));

        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("键盘"));
    }

    @Test
    @DisplayName("序列化后再反序列化，数据无损")
    void roundTrip() {
        Item original = new Item(7, "显示器", 3, 1299.0);

        Item restored = Json.fromJson(Json.toJson(original), Item.class);

        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getQuantity(), restored.getQuantity());
        assertEquals(original.getPrice(), restored.getPrice(), 1e-9);
    }

    @Test
    @DisplayName("列表序列化为 JSON 数组")
    void toJsonList() {
        String json = Json.toJson(List.of(new Item("A", 1, 1.0), new Item("B", 2, 2.0)));

        assertTrue(json.trim().startsWith("["));
    }
}
