package com.ohcp.project1.model;

/**
 * 示例领域对象。比赛时把字段换成题目要求的数据结构即可。
 */
public class Item {

    private long id;
    private String name;
    private int quantity;
    private double price;

    public Item() {
    }

    public Item(String name, int quantity, double price) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    public Item(long id, String name, int quantity, double price) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    /** 金额合计，演示"业务逻辑放在领域对象里"而不是散落在 main 里。 */
    public double total() {
        return quantity * price;
    }

    @Override
    public String toString() {
        return "Item{id=%d, name='%s', qty=%d, price=%.2f, total=%.2f}"
                .formatted(id, name, quantity, price, total());
    }
}
