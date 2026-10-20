package com.ohcp.project1;

import com.ohcp.project1.model.Item;
import com.ohcp.project1.repo.ItemRepository;
import com.ohcp.project1.service.ItemService;
import com.ohcp.project1.util.Json;

import java.sql.SQLException;
import java.util.List;

/**
 * 程序入口。这里只做「串起来」：解析输入 → 调 Service → 输出，不含业务逻辑。
 *
 * <p>运行：{@code mvn -q exec:java} 或 {@code java -jar target/ohcp-app.jar}
 * <p>Windows 控制台中文乱码时用 run.bat 启动，或先执行 {@code chcp 65001}。
 */
public class App {

    public static void main(String[] args) throws Exception {
        run();
    }

    /** 抽出成可测试方法：冒烟测试直接调它，验证整条链路。 */
    public static List<Item> run() throws SQLException {
        try (ItemRepository repo = new ItemRepository()) {
            ItemService service = new ItemService(repo);

            service.add("机械键盘", 2, 399.00);
            service.add("显示器", 1, 1299.00);
            service.add("鼠标垫", 5, 29.90);

            List<Item> all = service.list();
            System.out.println("=== 全部记录 ===");
            all.forEach(System.out::println);

            System.out.println("\n=== 金额 TOP2 ===");
            service.topByAmount(2).forEach(System.out::println);

            System.out.printf("%n=== 合计：%.2f 元 ===%n", service.grandTotal());

            System.out.println("\n=== JSON 输出 ===");
            System.out.println(Json.toJson(all));

            return all;
        }
    }
}
