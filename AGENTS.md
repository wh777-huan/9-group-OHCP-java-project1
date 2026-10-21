# OHCP 农作物缺素症识别与科学施肥建议智能体 — 协作协议

> 比赛时间：2026-10-21 现场演示 + 提交
> 比赛规则：60min 开发 + 30min 自愿答辩；下课铃前必须提交；75分提交+最高90分答辩
> 仓库：https://github.com/wh777-huan/OHCP-java-project1
> 分支约定：各人用自己的分支，合并到 main 后提交

---

## 一、项目基本信息

- 项目名称：农作物缺素症识别与科学施肥建议智能体
- 唯一开发仓库：https://github.com/wh777-huan/OHCP-java-project1
- 功能冻结时间：T+50（强制提交时间）
- 最终交付：源码（含爬虫/后端/前端）+ README + PPT + 测试证据

---

## 二、技术方案（严格按需求，不可改）

### 后端框架
- **SpringBoot 3.3.5** + **SpringAI 1.0.0-M6**
- 知识库检索：`KnowledgeBaseService`（`src/main/java/com/ohcp/project1/service/`）
- LLM 调用：`DiagnoseService`（通过 SpringAI `ChatClient` 接 DeepSeek）
- REST API：`DiagnoseController`（`src/main/java/com/ohcp/project1/controller/`）
- 启动入口：`OhcpApplication.java`（`src/main/java/com/ohcp/project1/`）

### 环境变量（必须设置，勿硬编码）
```bash
OHCP_LLM_API_KEY=your_deepseek_key
OHCP_LLM_BASE_URL=https://api.deepseek.com
OHCP_LLM_MODEL=deepseek-chat
```

### 知识库
- 路径：`src/main/resources/knowledge/nutrient_deficiency.json`
- 当前：8 种缺素症（N/P/K/Ca/Mg/Fe/Zn/B）
- 爬虫：`src/main/java/com/ohcp/project1/util/Crawler.java`（已有，可复用）

### 核心链路（合规）
```
前端症状 → /api/diagnose → KnowledgeBaseService（知识库优先）
                            → SpringAI ChatClient（DeepSeek 增强）
                            → 前端展示（缺素+置信度+施肥建议+思政）
```

### 前端
- 单 HTML：`src/main/resources/static/index.html`
- 启动后访问 http://localhost:8080

---

## 三、文件结构

```
src/main/
  java/com/ohcp/project1/
    OhcpApplication.java          ← SpringBoot 入口（已写）
    agent/AgentClient.java        ← 原 LLM 客户端（保留备用）
    controller/
      DiagnoseController.java     ← REST API（已写）
    service/
      KnowledgeBaseService.java   ← 知识库加载+检索（已写）
      DiagnoseService.java        ← 业务逻辑+SpringAI（已写）
    util/
      Crawler.java                ← 爬虫（已有）
  resources/
    knowledge/
      nutrient_deficiency.json    ← 知识库（已写，8种缺素症）
    application.properties        ← SpringBoot 配置（已写）
    static/
      index.html                  ← 前端页面（已写）

pom.xml                           ← SpringBoot + SpringAI（已写）
```

---

## 四、最小可验收链路

```
输入：水稻叶片从下部老叶开始均匀发黄，植株矮小，分蘖减少
预期输出：氮(N)缺乏 + 施肥建议 + 思政引导语
验收：整条链路可演示，终端无报错
```

---

## 五、分工（三角色）

| 角色 | 核心交付 | 状态 |
|------|---------|------|
| **知识库** | 扩充 nutrient_deficiency.json（>8种）+ 爬虫脚本 | 进行中 |
| **后端** | SpringBoot + SpringAI 全链路可跑 | ✅ 骨架完成 |
| **前端** | index.html 样式美化 + 截图证据 | 待认领 |
| **PPT** | 制作演示 PPT | 待认领 |

---

## 六、必须满足的规则

1. **知识库优先**：禁止 LLM 直接输出答案，必须先查本地 JSON
2. **T+50 强制提交**：代码必须可演示
3. **各人分支隔离**：各人在自己分支工作，不改他人文件
4. **提交前 `mvn test`**：保证代码可编译

---

## 七、状态报告格式

```
[STATUS｜2026-10-21 HH:MM]
- 版本/PR：
- 本阶段完成：
- 验证证据：
- 阻塞点：
- 下一步：
```
