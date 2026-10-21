package com.ohcp.agent.model;

/**
 * 前端分析请求。
 */
public class AnalyzeRequest {

    /** 用户输入的作物长势与缺素症状描述 */
    private String input;
    /** 输出模板：farmer 农户简易版 / tech 农技专业版 */
    private String mode = "farmer";

    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}
