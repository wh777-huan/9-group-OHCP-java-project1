# 农作物缺素症识别与科学施肥建议智能体

## 技术栈

- **后端**：SpringBoot 3.3.5 + SpringAI 1.0.0-M6（兼容 DeepSeek OpenAI 协议）
- **前端**：HTML + JS（轻量化页面，SpringBoot 静态资源）
- **知识库**：本地 JSON，爬虫采集公开农业数据构建
- **LLM**：DeepSeek（通过 SpringAI OpenAI 兼容接口调用）

## 快速启动

### 1. 配置环境变量

```bash
# Windows PowerShell
$env:OHCP_LLM_API_KEY="your_deepseek_api_key"
$env:OHCP_LLM_BASE_URL="https://api.deepseek.com"
$env:OHCP_LLM_MODEL="deepseek-chat"
```

### 2. 构建并运行

```bash
mvn clean package -DskipTests
java -jar target/ohcp-app.jar
```

或直接运行：

```bash
mvn spring-boot:run
```

### 3. 访问页面

浏览器打开：http://localhost:8080

## 核心链路

```
前端输入症状 → POST /api/diagnose
                        ↓
              KnowledgeBaseService（本地知识库检索）
                        ↓
              SpringAI ChatClient（DeepSeek 整理建议）
                        ↓
              前端展示缺素类型 + 置信度 + 施肥建议 + 思政引导
```

## 禁止事项

- ❌ 禁止绕过知识库直接让 LLM 输出答案
- ❌ 禁止将 API 密钥硬编码到源码

## 知识库

知识库位于 `src/main/resources/knowledge/nutrient_deficiency.json`，
当前包含 8 种常见缺素症数据：氮(N)、磷(P)、钾(K)、钙(Ca)、镁(Mg)、铁(Fe)、锌(Zn)、硼(B)。

## 人员分工

| 角色 | 负责人 |
|------|--------|
| 知识库构建与爬虫 | — |
| 后端 SpringBoot + SpringAI | 李幸欣 |
| 前端页面 | — |
| PPT 制作 | — |

> 详细协作协议见 `AGENTS.md`
