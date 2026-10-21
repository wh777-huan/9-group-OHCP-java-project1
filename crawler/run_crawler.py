# 农作物缺素症知识库爬虫
# 功能：采集公开的作物营养诊断与科学施肥科普资料 → 清洗 → 切片 → 按元素分类 → 增量写入本地知识库 knowledge.json
# 运行：python run_crawler.py
# 说明：允许网络不可用，自动降级为"内置专家种子数据"，保证知识库始终完整可用（现场演示零风险）。

import json
import os
import re
import sys
import time
from datetime import datetime

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
# 默认写入后端知识库目录，可通过 --output 覆盖
DEFAULT_OUTPUT = os.path.normpath(
    os.path.join(BASE_DIR, "..", "backend", "src", "main", "resources", "knowledge", "knowledge.json")
)
SEED_SOURCES_FILE = os.path.join(BASE_DIR, "seed_sources.txt")

# 元素关键词字典：用于把抓取到的文本切片自动分类到对应缺素条目
ELEMENT_KEYWORDS = {
    "N": ["缺氮", "氮素不足", "氮肥", "老叶发黄", "叶色淡绿", "均匀黄化", "黄化", "尿素", "碳铵", "铵态氮", "硝态氮", "叶绿素"],
    "P": ["缺磷", "磷素不足", "磷肥", "紫红色", "发紫", "暗绿色", "过磷酸钙", "磷酸二铵", "花青素", "磷固定"],
    "K": ["缺钾", "钾素不足", "钾肥", "叶缘焦枯", "叶尖焦枯", "灼烧", "氯化钾", "硫酸钾", "草木灰", "抗倒伏", "秸秆还田"],
    "Ca": ["缺钙", "钙素不足", "钙肥", "脐腐病", "苦痘病", "顶芽枯死", "新叶畸形", "氯化钙", "石膏", "干烧心"],
    "Mg": ["缺镁", "镁素不足", "镁肥", "叶脉间失绿", "鱼骨状", "硫酸镁", "脉间黄化", "叶脉绿"],
    "S": ["缺硫", "硫素不足", "硫肥", "新叶黄化", "硫酸铵", "含硫氨基酸", "十字花科"],
    "Fe": ["缺铁", "铁素不足", "铁肥", "新叶白化", "脉间失绿", "硫酸亚铁", "螯合铁", "黄叶病", "黄化", "石灰性土壤"],
    "Zn": ["缺锌", "锌素不足", "锌肥", "白苗病", "花白叶", "小叶病", "簇生", "节间缩短", "硫酸锌", "僵苗", "坐蔸"],
    "B": ["缺硼", "硼素不足", "硼肥", "花而不实", "蕾而不花", "硼砂", "生长点枯死", "木栓化", "茎裂"],
    "Mn": ["缺锰", "锰素不足", "锰肥", "硫酸锰", "脉间失绿", "褐斑", "斑点"],
    "Mo": ["缺钼", "钼素不足", "钼肥", "钼酸铵", "鞭尾", "根瘤", "固氮", "硝酸还原酶"],
    "Cu": ["缺铜", "铜素不足", "铜肥", "硫酸铜", "叶尖枯死", "白化", "穗而不实", "木质素"],
}


def load_seed_kb():
    """读取现有知识库（内置专家种子数据），作为增量更新的底稿。"""
    if os.path.exists(DEFAULT_OUTPUT):
        with open(DEFAULT_OUTPUT, "r", encoding="utf-8") as f:
            return json.load(f)
    raise FileNotFoundError("未找到 knowledge.json，请确认后端知识库文件存在")


def read_seed_sources():
    if not os.path.exists(SEED_SOURCES_FILE):
        return []
    urls = []
    with open(SEED_SOURCES_FILE, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#"):
                urls.append(line)
    return urls


def fetch_page(url, timeout=10):
    """抓取单个页面正文，返回 (title, [paragraphs])。"""
    try:
        import requests
        from bs4 import BeautifulSoup
    except ImportError:
        print("[提示] 缺少 requests/beautifulsoup4，跳过网络采集（使用离线模式）")
        return None, []

    try:
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                          "(KHTML, like Gecko) Chrome/120.0 Safari/537.36 CropAgent/1.0",
        }
        resp = requests.get(url, headers=headers, timeout=timeout)
        resp.encoding = resp.apparent_encoding or "utf-8"
        if resp.status_code != 200:
            print(f"  [跳过] {url} -> HTTP {resp.status_code}")
            return None, []
        soup = BeautifulSoup(resp.text, "html.parser")
        for tag in soup(["script", "style", "noscript", "nav", "footer", "aside", "iframe", "button", "form"]):
            tag.decompose()
        title = ""
        if soup.title:
            title = soup.title.get_text(strip=True)
        # 正文提取：优先取正文容器，其次取全部段落，最后取整页文本
        paragraphs = []
        body = (soup.find("article")
                or soup.find("div", class_=re.compile(r"(content|article|lemma|para|text|main|rich)", re.I))
                or soup)
        candidates = body.find_all("p") if body else []
        if not candidates:
            candidates = soup.find_all(["p", "li"])
        if not candidates:
            for div in soup.find_all("div"):
                t = clean_text(div.get_text(" ", strip=True))
                if len(t) > 30:
                    candidates.append(div)
        for p in candidates:
            text = p.get_text(" ", strip=True)
            text = clean_text(text)
            if text:
                paragraphs.append(text)
        # 去重（保留顺序）
        seen, uniq = set(), []
        for p in paragraphs:
            if p not in seen:
                seen.add(p)
                uniq.append(p)
        paragraphs = uniq
        print(f"  [成功] {url} -> 标题「{title[:40]}」，段落 {len(paragraphs)} 段")
        return title, paragraphs
    except Exception as e:
        print(f"  [失败] {url} -> {type(e).__name__}: {e}")
        return None, []


def clean_text(text):
    """数据清洗：去空白、去无关字符、去导航残留。"""
    text = re.sub(r"[\t\r\n]+", " ", text)
    text = re.sub(r"\s+", " ", text)
    text = re.sub(r"(版权所有|免责声明|点击查看|上一篇|下一篇|相关阅读|返回顶部|打印|字号|纠错|责任编辑|ICP备案|京ICP)", " ", text)
    return text.strip().strip("|").strip("-").strip()


def chunk_paragraphs(paragraphs, max_len=180):
    """文本切片：按段落聚合为长度适中的切片（用于向量化检索的文档单元）。"""
    chunks = []
    buf = ""
    for p in paragraphs:
        if len(p) < 12:  # 过滤过短噪音
            continue
        if len(buf) + len(p) <= max_len:
            buf = (buf + p) if not buf else buf + "。" + p
        else:
            if buf:
                chunks.append(buf)
            buf = p
    if buf:
        chunks.append(buf)
    return chunks


def classify_chunk(chunk):
    """把切片按关键词打分，归入最可能的缺素条目；返回 (元素id, 命中词列表)。"""
    best_id, best_score, best_hits = None, 0, []
    for eid, words in ELEMENT_KEYWORDS.items():
        hits = [w for w in words if w in chunk]
        if len(hits) > best_score:
            best_id, best_score, best_hits = eid, len(hits), hits
    return best_id, best_hits


def main():
    print("=" * 60)
    print("农作物缺素症知识库爬虫 开始运行")
    print("时间:", datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
    print("=" * 60)

    output_path = DEFAULT_OUTPUT
    if len(sys.argv) > 1 and sys.argv[1] == "--output" and len(sys.argv) > 2:
        output_path = os.path.abspath(sys.argv[2])

    kb = load_seed_kb()
    entries = {e["id"]: e for e in kb["entries"]}
    sources = read_seed_sources()
    print(f"知识库底稿：{len(entries)} 个缺素条目；采集源 {len(sources)} 个")

    fetch_ok = 0
    matched_total = 0
    if sources:
        for url in sources:
            title, paragraphs = fetch_page(url)
            if not paragraphs:
                continue
            fetch_ok += 1
            chunks = chunk_paragraphs(paragraphs)
            for ch in chunks:
                eid, hits = classify_chunk(ch)
                if eid and hits:
                    matched_total += 1
                    entry = entries[eid]
                    entry.setdefault("crawledNotes", []).append({
                        "url": url,
                        "title": title or url,
                        "snippet": ch[:120],
                        "hits": hits,
                        "fetchedAt": datetime.now().strftime("%Y-%m-%d"),
                    })
                    # 条目仅保留最近 20 条采集记录，避免知识库无限膨胀
                    entry["crawledNotes"] = entry["crawledNotes"][-20:]
            time.sleep(0.5)

    kb["source"] = "爬虫采集公开农技科普资料 + 农技推广公开数据清洗整理"
    kb["lastCrawlAt"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    kb["crawlSummary"] = {
        "sourcesTotal": len(sources),
        "fetchedOk": fetch_ok,
        "chunksMatched": matched_total,
        "mode": "在线采集" if fetch_ok else "离线兜底（内置专家种子数据）",
    }

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(kb, f, ensure_ascii=False, indent=2)
    print("-" * 60)
    print(f"采集成功 {fetch_ok}/{len(sources)} 个源，匹配切片 {matched_total} 条")
    print(f"知识库已写入：{output_path}")
    print(f"运行模式：{kb['crawlSummary']['mode']}")
    print("提示：切片已随条目落库；向量化与相似度检索由后端 TF-IDF 向量引擎完成（详见 backend）。")
    print("=" * 60)


if __name__ == "__main__":
    main()
