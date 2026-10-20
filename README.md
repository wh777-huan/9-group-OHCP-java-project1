# OHCP 物联网专业第一次课堂编程赛 · 项目 1

> 课程：面向对象程序设计（Java）  
> 赛制：OHCP（One Hour Class Programming）90 分钟现场限时开发  
> 小组：____ 组　成员：____ / ____ / ____ / ____

## 一、项目简介

（一句话说清这个系统解决什么问题、给谁用。）

本系统基于 **JDK 21 + Maven + H2 嵌入式数据库**，采用分层架构实现
「数据采集 → 业务处理 → 结果输出」的完整链路，并配套 JUnit 5 自动化测试与覆盖率报告。

## 二、环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 21 | `java -version` 验证 |
| Maven | 3.9.x | `mvn -version` 验证 |

无需安装数据库：H2 以 jar 形式内嵌，首次运行自动建表。

## 三、快速开始

```bash
# 1. 跑测试（含覆盖率报告）
mvn test

# 2. 打包
mvn package

# 3. 运行
java -jar target/ohcp-app.jar
```

Windows 下也可直接双击：

- `run.bat` —— 自动打包并运行（已处理中文乱码）
- `test.bat` —— 运行测试并提示覆盖率报告位置

覆盖率报告：`target/site/jacoco/index.html`

## 四、目录结构

```
ohcp-project1/
├── pom.xml                      # 依赖清单与构建配置
├── run.bat / test.bat           # 一键运行 / 一键测试
├── src/main/java/com/ohcp/project1/
│   ├── App.java                 # 入口：串流程，不含业务逻辑
│   ├── model/Item.java          # 领域对象：数据 + 自身行为（total()）
│   ├── repo/ItemRepository.java # 仓储层：只负责 SQL
│   ├── service/ItemService.java # 业务层：校验与聚合
│   ├── util/Crawler.java        # 爬虫：Jsoup 抓取与选择器抽取
│   ├── util/Json.java           # 统一 JSON 读写
│   └── agent/AgentClient.java   # 智能体：OpenAI 兼容接口调用
├── src/test/java/com/ohcp/project1/
│   ├── model/ItemTest.java
│   └── service/ItemServiceTest.java
└── docs/                        # 答辩材料与测试证据模板
```

## 五、关键模块说明

| 模块 | 职责 | 关键类 |
|------|------|--------|
| 领域模型 | 承载数据与自身业务行为，避免贫血模型 | `Item` |
| 仓储层 | 屏蔽数据库细节，对外只暴露集合语义 | `ItemRepository` |
| 业务层 | 输入校验、排序聚合、跨实体逻辑 | `ItemService` |
| 采集层 | 网页抓取、字段抽取 | `Crawler` |
| 智能体层 | 调用大模型完成理解/生成类任务 | `AgentClient` |

分层原则：**上层依赖下层，下层不知道上层存在**；入口类只做编排。

## 六、测试与覆盖率

`mvn test` 结果：**18 个用例，0 失败**；JaCoCo 实测 **行覆盖 83.7%（118/141）、分支覆盖 80.8%（21/26）**。

| 测试类 | 用例 | 覆盖点 |
|--------|------|--------|
| `ItemServiceTest` | 8 | 新增可见、三类非法输入、排序截断、聚合、模糊查询、空关键字 |
| `ItemTest` | 2 | 金额计算、零数量边界 |
| `CrawlerTest` | 4 | 选择器抽取、去空白、无匹配返回空、相对链接补全 |
| `JsonTest` | 3 | 序列化字段、往返无损、列表数组 |
| `AppTest` | 1 | 入口整链路冒烟 |

**关于 `AgentClient` 不在覆盖率内**：该类依赖外部大模型密钥与在线接口，单元测试无法稳定覆盖，
因此在 `pom.xml` 的 JaCoCo 配置中显式排除，而不是留着未覆盖的行拉低指标——这是有意的工程取舍，答辩时会主动说明。

> 现场执行 `test.bat` 或 `mvn test`，把结果与覆盖率页面截图放进「测试证据」材料。

## 七、常见问题

| 现象 | 处理 |
|------|------|
| 控制台中文乱码 | 用 `run.bat` 启动，或先执行 `chcp 65001` |
| `mvn` 不是内部命令 | 重开终端；或将 Maven 的 `bin` 目录加入 PATH |
| 依赖下载卡住 | 确认 `~/.m2/settings.xml` 中配置了阿里云镜像 |
| GitHub 无法访问 | 用 `https://gh-proxy.com/https://github.com/...` 前缀克隆 |

## 八、人员分工

| 成员 | 角色 | 负责内容 |
|------|------|----------|
| | 组长 / 架构 | 需求拆解、模块划分、进度协调 |
| | 开发 | 核心业务实现 |
| | 测试 | 用例设计、覆盖率、缺陷回归 |
| | 演讲 | PPT 与答辩主讲 |
| | 答辩手 | 抢答报名、质询应答 |
