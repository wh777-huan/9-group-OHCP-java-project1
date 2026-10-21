# 农作物缺素症识别与科学施肥建议智能体

> OHCP 一小时智能编程大赛参赛作品 · 科技助农 · 藏粮于地藏粮于技

一个前后端分离的智能体应用：用户输入作物长势与缺素症状描述，智能体基于**自建本地缺素症知识库（爬虫采集 + 清洗结构化 + RAG 检索增强）**识别缺素类型（多候选 + 置信度），并生成针对性科学施肥建议（农艺调控、有机/生物肥、化学追施、安全减量提醒），同时自然融入课程思政内容。

---

## 一、技术栈

| 层 | 技术 |
|---|---|
| 后端 | SpringBoot 3.5（Java 17+） |
| 智能体框架 | langchain4j 1.0（OpenAI 兼容接口） |
| 知识库 | 自建本地知识库 `knowledge.json`（内存集合 + TF-IDF 向量化余弦检索） |
| 爬虫 | Python（requests + BeautifulSoup）：采集 → 清洗 → 切片 → 分类入库 |
| 前端 | 原生 HTML + CSS + JS（零构建，由后端静态托管） |
| 一键启动 | `start.bat` / `java -jar`（便携 Maven/Git 已内置 `tools/`，无需安装环境） |

## 二、核心功能

1. **缺素症智能识别**：接收症状描述（作物类型、症状部位、症状表现、生长环境），输出多候选缺素类型 + 匹配置信度 + 匹配依据。
2. **科学施肥建议生成**：缺素简介、农艺调控手段、有机肥/生物肥施用方案、化学肥料追施方案、施肥安全与化肥减量增效提醒。
3. **RAG 检索增强**：业务识别逻辑依赖本地知识库（禁止完全依靠大模型），大模型仅做结果整理输出；无 API Key / 断网时自动降级为纯知识库规则输出，现场演示零风险。
4. **课程思政自然融入**：识别结果与施肥建议中顺带 1-2 行与农业、粮食安全、科技助农、绿色农业相关的简短引导，不弹窗、不说教。
5. **创新点**（已在 PPT 阐述）：
   - 场景创新：农户简易版 / 农技专业版两套输出模板
   - 场景创新：缺素症科普小卡片
   - 智能体能力创新：养分缺乏风险等级预警（结合土壤/环境输入）
   - 工程创新：高频缺素查询日志统计（农技大数据看板）

## 三、目录结构

```
crop-nutrient-agent/
├── README.md                     # 本文件
├── start.bat                     # Windows 一键启动（自动编译 + 启动 + 打开浏览器）
├── tools/                        # 本地便携环境（已 gitignore，不入库；README 提供下载方式）
├── crawler/                      # Python 爬虫
│   ├── run_crawler.py            # 采集→清洗→切片→分类→增量写入 knowledge.json
│   ├── seed_sources.txt          # 公开采集源配置
│   └── requirements.txt
└── backend/                      # SpringBoot + langchain4j 后端
    ├── pom.xml
    └── src/main/
        ├── java/com/ohcp/agent/  # 控制器、智能体编排、向量检索、渲染、统计
        └── resources/
            ├── application.yml             # 服务与大模型配置
            ├── knowledge/knowledge.json    # 本地缺素症知识库（12 类元素）
            └── static/                     # 前端页面（index.html / css / js）
```

## 四、快速启动（一键）

**方式 A（推荐，Windows）：双击 `start.bat`**

脚本自动完成：编译打包（首次较慢）→ 启动服务 → 打开浏览器 http://localhost:8080

**方式 B（手动）：**

```bat
cd backend
..\tools\apache-maven-3.9.16\bin\mvn.cmd -s ..\tools\maven-settings.xml spring-boot:run
```

或打包后直接运行（需先执行一次 `mvn package`）：

```bat
java -jar backend\target\crop-nutrient-agent.jar
```

启动成功后浏览器访问 **http://localhost:8080**，即可在页面输入症状体验完整链路：
`输入症状 → 后端接收 → 智能体检索本地知识库 → 生成施肥建议 → 前端展示`

## 五、大模型配置（可选增强）

默认配置了 DeepSeek（OpenAI 兼容接口）。编辑 `backend/src/main/resources/application.yml`：

```yaml
llm:
  enabled: true
  base-url: https://api.deepseek.com/v1   # 通义/智谱等改为对应 base-url
  api-key: sk-xxxxxxxx                    # 换成你的 API Key
  model-name: deepseek-chat
```

> 留空 `api-key` 自动降级为纯知识库规则输出，识别与施肥建议依然完整可用，适合现场无网络环境演示。

## 六、爬虫与知识库更新

```bat
cd crawler
pip install -r requirements.txt
python run_crawler.py
```

脚本会抓取 `seed_sources.txt` 中公开农技科普页面 → 清洗 → 切片 → 按元素关键词自动分类 → 增量合并写回
`backend/src/main/resources/knowledge/knowledge.json`（网络不可用时自动使用内置专家种子数据，知识库始终完整）。
支持新增缺素条目无需修改代码（知识库增量更新创新点）。

## 七、API 说明

| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/analyze` | POST | 症状识别与施肥建议 `{"input":"...","mode":"farmer\|tech"}` |
| `/api/stats` | GET | 高频缺素查询统计 TOP5 |
| `/api/health` | GET | 服务健康检查 |
| `/api/elements` | GET | 知识库缺素元素清单 |

示例：

```bash
curl -X POST http://localhost:8080/api/analyze -H "Content-Type: application/json" \
  -d "{\"input\":\"水稻，叶片从下部老叶开始均匀发黄，植株矮小，分蘖减少。\",\"mode\":\"farmer\"}"
```

## 八、环境要求

- JDK 17+（本项目在 JDK 25 验证通过）
- 无需安装 Maven / Git（已内置便携版到 `tools/`）
- 首次启动需联网下载 Maven 依赖（已配置阿里云镜像）；运行阶段不依赖外部网络

## 九、常见问题

| 问题 | 解决 |
|---|---|
| 8080 端口被占用 | 修改 `application.yml` 的 `server.port` 后重启 |
| 首次启动很慢 | Maven 正在下载依赖，等待即可；后续秒级启动 |
| 提示"服务未连接" | 等待控制台出现 `Started CropNutrientAgentApplication` 后刷新页面 |
| 想切换大模型厂商 | 修改 `application.yml` 的 `base-url` 与 `model-name`（OpenAI 兼容均可） |
