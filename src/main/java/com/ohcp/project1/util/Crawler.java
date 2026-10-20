package com.ohcp.project1.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 爬虫工具：Jsoup 抓取 + 选择器提取。
 *
 * <p>抓取与解析刻意拆开：{@link #fetch(String)} 负责网络，{@code parse*} 负责解析。
 * 这样解析逻辑可以脱离网络做单元测试，现场断网也能验证正确性。
 */
public class Crawler {

    private final int timeoutMillis;

    public Crawler() {
        this(8000);
    }

    public Crawler(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public Document fetch(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) OHCP-Scraper/1.0")
                .timeout(timeoutMillis)
                .ignoreHttpErrors(true)
                .get();
    }

    public List<String> texts(String url, String cssSelector) throws IOException {
        return parseTexts(fetch(url), cssSelector);
    }

    public List<String> links(String url, String cssSelector) throws IOException {
        return parseLinks(fetch(url), cssSelector);
    }

    /** 从 HTML 片段抽取文本，不触网。 */
    public static List<String> parseTexts(String html, String cssSelector) {
        return parseTexts(Jsoup.parse(html), cssSelector);
    }

    public static List<String> parseTexts(Document doc, String cssSelector) {
        Elements els = doc.select(cssSelector);
        List<String> out = new ArrayList<>(els.size());
        for (Element el : els) {
            out.add(el.text().trim());
        }
        return out;
    }

    /** 从 HTML 片段抽取链接，不触网。 */
    public static List<String> parseLinks(String html, String baseUri, String cssSelector) {
        Document doc = baseUri == null ? Jsoup.parse(html) : Jsoup.parse(html, baseUri);
        return parseLinks(doc, cssSelector);
    }

    public static List<String> parseLinks(Document doc, String cssSelector) {
        Elements els = doc.select(cssSelector);
        List<String> out = new ArrayList<>(els.size());
        for (Element el : els) {
            out.add(el.absUrl("href"));
        }
        return out;
    }
}
