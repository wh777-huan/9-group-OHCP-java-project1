# OHCP 农作物缺素症识别与科学施肥建议智能体 — 协作协议

> 比赛时间：2026-10-21 T+0 ~ T+60min，现场演示 + 提交
> 比赛规则：60min 开发 + 30min 自愿答辩；下课铃前必须提交；75分提交+最高90分答辩
> 仓库：https://github.com/wh777-huan/OHCP-java-project1
> 你的分支：lixingxin（建仓时间：2026-10-21 08:38）
> 我的分支：lixingxin（与你的分支相同，同一账号 Cynthia060913）
> 第三人：wh777-huan（owner，主仓库，可 merge PR）

---

## 一、项目基本信息

- 项目名称：农作物缺素症识别与科学施肥建议智能体
- 唯一开发仓库：https://github.com/wh777-huan/OHCP-java-project1
- 最终交付要求及截止日期：2026-10-21 现场演示可运行程序 + 提交源码 + README + PPT
- 功能冻结时间：T+50（强制提交时间）
- 你的角色：**协调 Agent / 主开发（本人李幸欣的 AI 助手）**
- 我负责的模块范围：**全栈（知识库+后端+前端+集成）**，爬虫可并行

---

## 二、技术方案（已确认，不可改）

### 后端
- 框架：Java 原生 `HttpServer`（JDK 内置，无 SpringBoot，1小时内可跑通）
- 知识库：爬虫采集 → 清洗 → `src/main/resources/knowledge/nutrient_deficiency.json`
- RAG 流程：输入症状 → 关键词/向量匹配本地知识库 → 候选结果 → DeepSeek 整理输出
- LLM：AgentClient.java（接 DeepSeek，OpenAI 兼容 API，已实测可用）
- 禁止：直接让 LLM 输出答案而不经过本地知识库检索（需求明确要求）

### 知识库数据结构
```json
{
  "deficiencies": [
    {
      "id": "N-001",
      "element": "氮(N)",
      "symptoms": ["下部老叶先发黄", "植株矮小", "分蘖减少"],
      "description": "氮是叶绿素组成元素...",
      "organic_fertilizer": "腐熟农家肥每亩...",
      "chemical_fertilizer": "尿素追施...",
      "safety_note": "切忌过量撒施..."
    }
  ]
}
```

### 前端
- 单个 HTML 文件：`src/main/resources/static/index.html`
- 流程：输入症状 → POST /api/diagnose → 展示缺素类型 + 置信度 + 施肥建议

### 思政融入（自然输出，不弹窗）
- 施肥建议末尾顺带输出"藏粮于地、藏粮于技"等简短引导语（1-2行）
- 不单独弹窗，不生硬说教

---

## 三、最小可验收链路（第一优先）

```
用户输入 → 前端 → POST /api/diagnose
                        ↓
              知识库检索（症状匹配）
                        ↓
              DeepSeek 整理建议（AgentClient）
                        ↓
              前端展示结果
```

**验收条件**：用户输入"水稻叶片从下部老叶开始均匀发黄，植株矮小，分蘖减少"，前端返回氮缺乏 + 施肥建议，整条链路可演示。

---

## 四、分工建议（三角色）

| 角色 | 负责人 | 核心交付 |
|------|--------|---------|
| **知识库 Agent** | 你（或队友） | 采集并清洗至少 8 种缺素症数据到 JSON |
| **推理引擎 Agent** | QClaw | 检索逻辑 + AgentClient RAG + HttpServer API |
| **前端+集成 Agent** | 队友（或 Claude Code） | HTML 页面 + API 调用 + 样式 |

> 实际比赛中三人均用同一 GitHub 账号（Cynthia060913），建议通过分支隔离 + PR 合并：
> - `knowledge-base` 分支：知识库 JSON
> - `engine` 分支：后端检索 + RAG
> - `frontend` 分支：HTML 页面
> - `lixingxin` 分支：你的分支（可与上述合并）
>
> 最终由一人（或 QClaw 代操作）合并到 main 后提交

---

## 五、文件结构（目标）

```
src/main/
  java/com/ohcp/project1/
    util/
      Crawler.java         ← 爬虫（已有，可复用）
      KnowledgeBase.java    ← 知识库加载+检索
    agent/
      AgentClient.java     ← LLM 调用（已有，可复用）
    service/
      DiagnoseService.java ← 诊断业务逻辑
    api/
      DiagnoseHandler.java  ← HttpServer 请求处理
    App.java               ← 入口，启动 HttpServer
  resources/
    knowledge/
      nutrient_deficiency.json  ← 知识库（需爬虫填充）
    static/
      index.html           ← 前端页面
      style.css            ← 样式
      app.js               ← 前端逻辑

src/test/
  java/com/ohcp/project1/
    KnowledgeBaseTest.java  ← 知识库检索测试
    DiagnoseServiceTest.java
```

---

## 六、必须满足的规则（不可违反）

1. **知识库优先**：禁止 LLM 直接回答；必须先查本地 JSON 知识库
2. **可演示**：T+50 时整个链路必须可运行，支持现场演示
3. **不破坏队友成果**：各人在自己分支工作，不改他人文件
4. **提交前跑 `mvn test`**：保证代码可编译、可运行
5. **README 包含**：环境依赖、启动说明、依赖清单、人员分工

---

## 七、状态报告格式

每完成一个里程碑，评论格式：

```
[STATUS｜2026-10-21 HH:MM]
- 版本/PR：
- 本阶段完成：
- 实际验证及证据：
- 未完成：
- 阻塞点：
- 下一步及预计时间：
```
