package com.ohcp.project1.repo;

import com.ohcp.project1.model.Item;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 H2 嵌入式数据库的仓储层。
 *
 * <p>用内存库（jdbc:h2:mem:...）是为了比赛现场免安装、免配置、不占端口。
 * 需要持久化时把 URL 换成 {@code jdbc:h2:./data/ohcp} 即可，目录会自动创建。
 */
public class ItemRepository implements AutoCloseable {

    private final Connection conn;

    public ItemRepository() {
        this("jdbc:h2:mem:ohcp;DB_CLOSE_DELAY=-1");
    }

    public ItemRepository(String jdbcUrl) {
        try {
            this.conn = DriverManager.getConnection(jdbcUrl, "sa", "");
            initSchema();
        } catch (SQLException e) {
            throw new IllegalStateException("数据库初始化失败: " + jdbcUrl, e);
        }
    }

    private void initSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS item (
                        id       BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name     VARCHAR(128) NOT NULL,
                        quantity INT          NOT NULL,
                        price    DECIMAL(12,2) NOT NULL
                    )
                    """);
        }
    }

    public Item insert(Item item) throws SQLException {
        String sql = "INSERT INTO item(name, quantity, price) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getName());
            ps.setInt(2, item.getQuantity());
            ps.setDouble(3, item.getPrice());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    item.setId(keys.getLong(1));
                }
            }
        }
        return item;
    }

    public List<Item> findAll() throws SQLException {
        List<Item> items = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name, quantity, price FROM item ORDER BY id")) {
            while (rs.next()) {
                items.add(new Item(rs.getLong("id"), rs.getString("name"),
                        rs.getInt("quantity"), rs.getDouble("price")));
            }
        }
        return items;
    }

    public List<Item> findByName(String keyword) throws SQLException {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT id, name, quantity, price FROM item WHERE name LIKE ? ORDER BY id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new Item(rs.getLong("id"), rs.getString("name"),
                            rs.getInt("quantity"), rs.getDouble("price")));
                }
            }
        }
        return items;
    }

    public int deleteAll() throws SQLException {
        try (Statement st = conn.createStatement()) {
            return st.executeUpdate("DELETE FROM item");
        }
    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException ignored) {
            // 关闭失败不影响退出
        }
    }
}
