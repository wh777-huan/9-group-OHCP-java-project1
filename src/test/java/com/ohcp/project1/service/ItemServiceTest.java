package com.ohcp.project1.service;

import com.ohcp.project1.model.Item;
import com.ohcp.project1.repo.ItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 业务层测试：每个用例一个断言主题，命名写清「输入 → 期望」。
 * 答辩时直接打开这份文件讲覆盖率，比嘴上说「我们测过」有说服力。
 */
class ItemServiceTest {

    private ItemRepository repo;
    private ItemService service;

    @BeforeEach
    void setUp() {
        // 每个用例独立内存库，用例之间互不污染
        repo = new ItemRepository("jdbc:h2:mem:test-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        service = new ItemService(repo);
    }

    @AfterEach
    void tearDown() {
        repo.close();
    }

    @Test
    @DisplayName("新增合法记录后能查到，且 id 非 0")
    void addThenList() throws Exception {
        Item saved = service.add("键盘", 2, 100.0);

        assertTrue(saved.getId() > 0);
        List<Item> all = service.list();
        assertEquals(1, all.size());
        assertEquals("键盘", all.get(0).getName());
    }

    @Test
    @DisplayName("名称为空 → 抛 IllegalArgumentException")
    void rejectBlankName() {
        assertThrows(IllegalArgumentException.class, () -> service.add("  ", 1, 10.0));
    }

    @Test
    @DisplayName("数量非正数 → 抛 IllegalArgumentException")
    void rejectNonPositiveQuantity() {
        assertThrows(IllegalArgumentException.class, () -> service.add("键盘", 0, 10.0));
    }

    @Test
    @DisplayName("单价为负 → 抛 IllegalArgumentException")
    void rejectNegativePrice() {
        assertThrows(IllegalArgumentException.class, () -> service.add("键盘", 1, -0.01));
    }

    @Test
    @DisplayName("topByAmount 按金额倒序，且只返回 n 条")
    void topByAmountOrdersDescending() throws Exception {
        service.add("A", 1, 10.0);   // 10
        service.add("B", 3, 100.0);  // 300
        service.add("C", 2, 50.0);   // 100

        List<Item> top = service.topByAmount(2);

        assertEquals(2, top.size());
        assertEquals("B", top.get(0).getName());
        assertEquals("C", top.get(1).getName());
    }

    @Test
    @DisplayName("grandTotal 等于各条 total 之和")
    void grandTotalSumsAll() throws Exception {
        service.add("A", 2, 10.0);
        service.add("B", 1, 5.0);

        assertEquals(25.0, service.grandTotal(), 1e-9);
    }

    @Test
    @DisplayName("模糊搜索命中名字包含关键字的记录")
    void searchMatchesKeyword() throws Exception {
        service.add("机械键盘", 1, 399.0);
        service.add("显示器", 1, 1299.0);

        List<Item> hit = service.search("键盘");

        assertEquals(1, hit.size());
        assertEquals("机械键盘", hit.get(0).getName());
    }

    @Test
    @DisplayName("搜索关键字为空 → 返回全部")
    void searchBlankReturnsAll() throws Exception {
        service.add("A", 1, 1.0);
        service.add("B", 1, 1.0);

        assertEquals(2, service.search("").size());
    }
}
