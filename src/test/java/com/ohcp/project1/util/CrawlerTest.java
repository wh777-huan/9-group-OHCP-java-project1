package com.ohcp.project1.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 解析逻辑脱离网络测试，现场断网也能跑。 */
class CrawlerTest {

    private static final String HTML = """
            <html><body>
              <ul>
                <li class="title"><a href="/p/1">  第一条  </a></li>
                <li class="title"><a href="/p/2">第二条</a></li>
              </ul>
              <div class="price">￥99</div>
              <div class="price">￥199</div>
            </body></html>
            """;

    @Test
    @DisplayName("按选择器抽取文本，且去除首尾空白")
    void parseTextsTrims() {
        List<String> titles = Crawler.parseTexts(HTML, "li.title");

        assertEquals(List.of("第一条", "第二条"), titles);
    }

    @Test
    @DisplayName("抽取多组同选择器文本")
    void parseTextsMultiple() {
        assertEquals(List.of("￥99", "￥199"), Crawler.parseTexts(HTML, "div.price"));
    }

    @Test
    @DisplayName("选择器无匹配 → 返回空列表而非 null")
    void parseTextsNoMatchReturnsEmpty() {
        List<String> none = Crawler.parseTexts(HTML, "table.missing");

        assertTrue(none.isEmpty());
    }

    @Test
    @DisplayName("带 baseUri 时 href 补全为绝对地址")
    void parseLinksResolvesAbsoluteUrl() {
        List<String> links = Crawler.parseLinks(HTML, "https://example.com", "li.title a");

        assertEquals(List.of("https://example.com/p/1", "https://example.com/p/2"), links);
    }
}
