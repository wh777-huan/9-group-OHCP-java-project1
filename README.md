# 农作物缺素症识别与科学施肥建议智能体

> OHCP 一小时智能编程大赛 · 物联网专业  
> 题目：见 `OHCP农作物缺素症识别与科学施肥建议智能体需求文档V1.0`

**当前状态：可启动骨架**。技术栈、配置、接口、前端页面、测试框架都已就位并能跑通；
业务逻辑（检索算法、知识库内容、施肥建议、思政文案）留空，由小组现场实现。

---

## 一、快速开始

```bash
# 方式一：双击
start.bat

# 方式二：命令行
mvn spring-boot:run

# 方式三：打包后运行
mvn package && java -jar target/ohcp-agri-agent.jar
```

浏览器打开 **http://localhost:8080**

### 配置大模型（可选，但题目要求大模型参与「整理输出」）

密钥走环境变量，**不要写进代码或配置文件**：

```cmd
set OHCP_LLM_API_KEY=你的key
```

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `OHCP_LLM_API_KEY` | 无 | 必填，否则调用大模型会失败 |
| `OHCP_LLM_BASE_URL` | `https://api.deepseek.com` | 通义填 `https://dashscope.aliyuncs.com/compatible-mode`；本地 Ollama 填 `http://localhost:11434` |
| `OHCP_LLM_MODEL` | `deepseek-chat` | 如 `qwen-plus`、`llama3` |

> 不配 key 也能启动、能跑检索链路 —— 只有「大模型整理输出」这一步会失败。
> 这正好符合题目「业务核心识别逻辑必须依赖本地知识库」的要求。

---

## 二、技术栈（对应「技术规范落地」30 分）

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.5.16 | Web + 一键启动 |
| Spring AI | 1.1.8 | `ChatClient` 接 OpenAI 兼容接口 |
| langchain4j | 1.22.0 | 智能体框架（题目二选一，选了它） |
| Jsoup | 1.18.3 | 爬虫 |
| H2 | 随 Boot | 知识库可选持久化方式 |
| JUnit 5 + JaCoCo | — | 测试与覆盖率 |

依赖已全部预热到本地仓库，**断网也能构建**（`mvn -o test` 已验证）。

---

## 三、已就位 / 待实现

### 已就位 ✅

- [x] Spring Boot 工程，一键启动，Tomcat 8080
- [x] Spring AI `ChatClient` 与 langchain4j 均已进入上下文
- [x] `POST /api/diagnose` 与 `GET /api/health` 接口
- [x] 前端页面 `index.html`：症状输入表单 + 结果分区块回显（含思政区块样式）
- [x] 知识库接口抽象 `KnowledgeBase` + JSON 载入实现
- [x] 4 个自检用例（上下文、健康检查、接口形状、请求拼句）
- [x] 中文编码处理、离线构建

### 待实现 ⬜（这是你们的工作量）

| # | 位置 | 要做什么 | 对应分数 |
|---|------|----------|----------|
| 1 | `crawler/` | 爬取公开农业资料 → 清洗 → 切片 → 生成 `data/knowledge.json` | 技术规范 |
| 2 | `InMemoryKnowledgeBase#search` | 相似度/关键词检索与排序 | 技术规范 + 业务 |
| 3 | `DiagnosisAgent#diagnose` | 聚合候选 + 拼 RAG 提示词 + 调大模型 | 业务 35 |
| 4 | 同左 | 思政文案自然融入输出 | 思政 20 |
| 5 | 任选 | 至少 1 个创新点 | 创新 10 |
| 6 | `docs/` | PPT 与 README 完善 | 文档 5 |

---

## 四、目录结构

```
ohcp-agri-agent/
├── pom.xml
├── start.bat
├── src/main/java/com/ohcp/agri/
│   ├── AgriAgentApplication.java   # 入口
│   ├── agent/DiagnosisAgent.java   # 编排：检索 → 提示词 → 大模型（待实现）
│   ├── kb/
│   │   ├── KnowledgeBase.java      # 检索接口（换存储不改业务）
│   │   ├── KnowledgeChunk.java     # 切片数据结构
│   │   └── InMemoryKnowledgeBase.java  # JSON 载入 + 检索（检索待实现）
│   └── web/
│       ├── DiagnosisController.java
│       └── dto/{DiagnosisRequest,DiagnosisResponse}.java
├── src/main/resources/
│   ├── application.yml
│   ├── data/knowledge.json         # 知识库（当前为空，等爬虫生成）
│   └── static/index.html           # 前端
└── src/test/java/com/ohcp/agri/AgriAgentApplicationTests.java
```

---

## 五、知识库数据结构

`data/knowledge.json` 是一个数组，每条形如：

```json
{
  "id": "rice-n-001",
  "crop": "水稻",
  "element": "氮",
  "symptom": "下部老叶均匀发黄,植株矮小,分蘖减少",
  "text": "水稻缺氮时，植株矮小，分蘖少，叶片自下而上均匀褪绿发黄……",
  "sourceUrl": "https://（你抓取的公开资料链接）"
}
```

字段可增删，但 `text` 必须是用于相似度计算的正文，`sourceUrl` 建议保留 ——
答辩时用它证明数据确实来自自建爬虫，而不是大模型编的。

---

## 六、评分对照自检

| 维度 | 分值 | 骨架现状 | 你们要补的 |
|------|------|----------|------------|
| 业务功能实现 | 35 | 接口通，返回占位 | 识别 + 建议完整链路 |
| 技术规范落地 | 30 | 栈齐、架构对、能一键启动 | 爬虫产出知识库 + RAG 检索生效 |
| 课程思政融入 | 20 | 前端预留思政区块 | 1–2 行随业务输出的文案 |
| 创新设计 | 10 | — | 至少 1 项，PPT 讲清 |
| 文档与交付物 | 5 | README 框架已有 | 补实际截图与启动说明 |

---

## 七、常见问题

| 现象 | 处理 |
|------|------|
| 8080 端口被占用 | 改 `application.yml` 里的 `server.port`，或关掉占用进程 |
| 调用大模型报 401 | 检查 `OHCP_LLM_API_KEY` 是否设置、是否在当前终端生效（重开终端） |
| 调用大模型报 404 | `OHCP_LLM_BASE_URL` 填错；DeepSeek 不要带 `/v1`，Spring AI 会自己补 |
| 知识库一直是 0 条 | `data/knowledge.json` 还没生成，或 JSON 格式不合法（看启动日志） |
| 中文乱码 | 用 `start.bat` 启动；或先 `chcp 65001` |
| 断网构建失败 | 确认 `~/.m2/settings.xml` 配了阿里云镜像，且用 `mvn -o` |
