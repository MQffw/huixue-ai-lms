# 🎓 慧学通智能教学管理系统

<div align="center">

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.13-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0--M6-blue.svg)](https://docs.spring.io/spring-ai)
[![Java](https://img.shields.io/badge/Java-18-blue.svg)](https://www.oracle.com/java)
[![Vue](https://img.shields.io/badge/Vue.js-3.x-4FC08D.svg)](https://vuejs.org)
[![MySQL](https://img.shields.io/badge/MySQL-5.5+-orange.svg)](https://www.mysql.com)
[![Redis](https://img.shields.io/badge/Redis-5.x-red.svg)](https://redis.io)
[![License](https://img.shields.io/badge/license-MIT-yellow.svg)](LICENSE)

**基于 Spring Boot 3.5 + Vue 3 + Spring AI 的现代化培训管理系统 —— 以 AI Agent 为核心的智能教务中台**

[API 文档](API接口文档.md) · [问题反馈](https://github.com/MQffw/web-ai-project02/issues)

</div>

---

## 项目简介

慧学通是一款面向培训机构的智能教学管理系统，集成了基于 Spring AI 的 AI Agent 智能助手，支持自然语言查询业务数据、知识库 RAG 检索、多模型切换与自动降级。

系统覆盖教务管理全链路：部门/员工/班级/学员管理、课程/排课/成绩/考勤/违纪/缴费/数据统计，并通过 AI Agent 让用户用自然语言完成数据查询。

---

## 功能特性

### 核心业务

| 模块 | 说明 |
|------|------|
| 部门管理 | 组织架构维护、部门增删改查 |
| 员工管理 | 员工档案、工作经历、职位薪资 |
| 班级管理 | 班级信息、班主任分配、学科管理 |
| 学员管理 | 学员档案、班级分配、违纪记录 |
| 课程管理 | 课程信息、学科分类、课时管理 |
| 排课管理 | 班级课表、教师安排、教室分配 |
| 成绩管理 | 考试成绩、排名统计、班级均分 |
| 考勤管理 | 出勤记录、出勤率、异常预警 |
| 缴费管理 | 缴费记录、欠费统计、班级缴费率 |
| 就业管理 | 就业跟踪、薪资统计、就业率 |
| 通知公告 | 通知/公告/制度文档管理 |
| 操作日志 | 系统操作审计追踪 |
| 文件上传 | 支持图片等资源上传，集成云存储 |

### AI 智能助手

```
┌─────────────┐    POST /ai/chat (SSE)    ┌──────────────────┐
│   前端       │ ──────────────────────────→ │ AiChatController │
└─────────────┘                             └────────┬─────────┘
                                                     │
                                                     ▼
                                          ┌──────────────────────┐
                                          │  AiOrchestrator      │
                                          │  (统一编排层)         │
                                          └────────┬─────────────┘
                                                   │
              ┌──────────┬──────────┬───────────────┼──────────────┐
              ▼          ▼          ▼               ▼              ▼
         ┌────────┐ ┌───────┐ ┌────────┐    ┌──────────┐   ┌──────────┐
         │ Guard  │ │Memory │ │Intent  │    │   Tool   │   │   RAG    │
         │安全拦截│ │4层记忆│ │Router  │    │ Function │   │ 知识检索 │
         └────────┘ └───────┘ └────────┘    └──────────┘   └──────────┘
```

#### AI 核心能力

- **安全拦截** — Prompt Injection 正则匹配，中英文攻击模式全覆盖
- **意图路由** — 规则优先分流（统计/查询/RAG/生成/闲聊），6 类域工具按需注入
- **Function Calling** — Spring AI 原生工具调用，模型自动选择并执行业务工具
- **知识库 RAG** — 文档上传 → 解析分块 → MySQL FULLTEXT 检索 → Re-rank Top3
- **语义记忆** — 短期 Redis + LLM 摘要 + 语义召回 + 用户画像
- **多模型切换** — DeepSeek / Mimo / LongCat 三模型 + 故障自动降级
- **流式输出** — 无工具意图 token 级真流式；工具意图先执行工具再分块推送（SSE 打字机效果），首字延迟 < 2s
- **数据脱敏** — 流式链路中实时脱敏手机/身份证号
- **会话隔离** — 前端生成 sessionId，每个对话独立存储
- **异步持久化** — 对话记录异步写入 MySQL，不阻塞响应

#### AI 用量统计（观测面板）

- 24 小时 Token 用量柱状图
- 模型 Token 占比饼图
- 总览卡片（总 Token / Prompt / Completion / 会话数 / 用户数）

### 安全特性

- **JWT Token 认证** — 无状态认证，自动续期
- **接口权限控制** — 拦截器 + `@RequiresRole` 注解 + AOP 数据权限（ADMIN/TEACHER/STUDENT）
- **操作日志** — AOP 自动记录到 `emp_log`（不存对话内容）
- **SQL 多层安全** — 白名单表、DDL/DML 拦截、注入防御、子查询深度限制
- **数据验证** — 前后端双重数据验证（JSR303）
- **限流防护** — 普通接口 100 次/秒，AI 接口 10 次/秒

---

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 18 | 编程语言 |
| Spring Boot | 3.5.13 | 应用框架 |
| Spring AI | 1.0.0-M6 | AI 框架（Function Calling + 流式） |
| MyBatis | 3.5.19 | ORM 框架 |
| MySQL | 5.5+ | 数据库 |
| Redis | 5.x+ | 对话历史 + 缓存（24h TTL） |
| JWT | 0.9.1 | 认证方案 |
| DeepSeek / Mimo / LongCat | - | 多模型 + 自动降级 |

### 前端

| 技术 | 说明 |
|------|------|
| Vue 3 | 渐进式 JavaScript 框架 |
| Vite | 极速构建工具 |
| Element Plus | Vue 3 UI 组件库 |
| ECharts | 可视化图表库 |
| Axios | HTTP 客户端 |
| Vue Router | 路由管理 |
| Pinia | 状态管理 |

### AI 模型

| 模型 | 提供商 | 说明 |
|------|--------|------|
| DeepSeek V4 Pro | DeepSeek | 深度推理模型 |
| Mimo V2.5 Pro | 小米 | 推理模型（含 reasoning_tokens） |
| LongCat 2.0 | 美团 | 默认模型 |

---

## 快速开始

### 环境要求

- JDK 18+
- Maven 3.6+
- MySQL 5.5+
- Redis 3.x+（可选）
- AI API Key（环境变量 `deepseek-api` / `mimo-api` / `longcat-api`）

### 1. 克隆项目

```bash
git clone https://github.com/MQffw/web-ai-project02.git
cd web-ai-project02
```

### 2. 数据库初始化

```sql
CREATE DATABASE tlias DEFAULT CHARACTER SET utf8;
SOURCE tlias-web-management/src/main/resources/db/tlias_extend.sql;
```

### 3. 启动 Redis

```bash
redis-server
```

默认连接 `localhost:6379`，可在 `application.yml` 中修改。

### 4. 配置环境变量

```bash
# Linux/Mac
export longcat-api=your-api-key
export deepseek-api=your-api-key
export mimo-api=your-api-key

# Windows PowerShell
$env:longcat-api="your-api-key"
$env:deepseek-api="your-api-key"
$env:mimo-api="your-api-key"
```

### 5. 配置

编辑 `tlias-web-management/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tlias
    username: root
    password: 123456
  ai:
    openai:
      api-key: ${longcat-api}
      base-url: https://api.longcat.chat/openai

ai:
  models:
    deepseek:
      api-key: ${deepseek-api}
      model: deepseek-v4-pro
    mimo:
      api-key: ${mimo-api}
      model: mimo-v2.5-pro
    longcat:
      api-key: ${longcat-api}
      model: LongCat-2.0
  max-tokens: 1024
  max-tokens-tool: 2048
```

### 6. 启动

```bash
cd tlias-web-management
mvn spring-boot:run
```

### 7. 访问

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:8081 |
| 后端 API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |

### 8. 测试账号

| 用户名 | 密码 | 权限 |
|--------|------|------|
| songjiang | 123456 | 管理员 |
| lujunyi | 123456 | 管理员 |

---

## 项目结构

```
web-ai-project02/
├── README.md                          # 项目说明
├── API接口文档.md                      # 完整 API 文档
├── tlias-web-management/              # 后端项目
│   ├── src/main/java/com/itheima/
│   │   ├── ai/                        # AI 模块
│   │   │   ├── orchestrator/          # AiOrchestratorService - 统一编排层
│   │   │   ├── tool/                  # 6 个域 Tool 类
│   │   │   ├── advisor/               # Advisor 链（SafeGuard/TokenTracking）
│   │   │   ├── memory/                # 语义记忆（短期/摘要/画像）
│   │   │   ├── rag/                   # RAG 知识检索
│   │   │   ├── validator/             # 答案一致性校验
│   │   │   ├── prompt/                # Prompt 模块（7 个模板）
│   │   │   ├── IntentRouter.java      # 规则优先意图路由
│   │   │   ├── Intent.java            # 意图枚举
│   │   │   ├── KnowledgeBaseService.java
│   │   │   └── DataMasker.java        # 数据脱敏
│   │   ├── controller/                # 控制器
│   │   ├── service/                   # 业务 Service
│   │   ├── mapper/                    # MyBatis Mapper
│   │   ├── aop/                       # 操作日志切面
│   │   ├── security/sql/              # SQL 安全模块
│   │   └── config/                    # 多模型 ChatClient 工厂
│   ├── src/main/resources/
│   │   ├── prompts/                   # 按意图拆分的 Prompt 模板
│   │   ├── db/tlias_extend.sql        # AI 模块扩展表
│   │   └── application.yml            # 配置文件
│   └── pom.xml
└── front-dist/                        # Vue 3 前端构建产物
```

---

## AI 模块扩展表

| 表名 | 用途 | 关键字段 |
|------|------|----------|
| `tlias_knowledge_doc` | 知识库文档主表 | file_name / status / chunk_count |
| `tlias_knowledge_chunk` | 文档分块（RAG 核心） | content / embedding_json |
| `tlias_token_usage` | Token 统计 | prompt_tokens / completion_tokens / total_tokens |
| `tlias_user_ai_profile` | 用户画像 | favorite_tools / common_intents |

---

## API 参考

### 认证说明

登录后获取 JWT 令牌，后续请求需在 Header 中携带：

```
token: eyJhbGciOiJIUzI1NiJ9...
```

### AI 助手

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/ai/chat` | 流式对话（SSE） |
| POST | `/ai/chat/sync` | 同步对话（测试用） |
| GET | `/ai/history?sessionId=xxx` | 加载对话历史 |

**请求体：**

```json
{
  "message": "各学历学生人数统计",
  "modelType": "longcat",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "history": []
}
```

### AI 统计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/ai-stats/overview` | 总览统计 |
| GET | `/ai-stats/tokens-24h` | 24 小时 Token 趋势 |
| GET | `/ai-stats/model-distribution` | 模型 Token 占比 |

### 业务接口

| 模块 | 路径 |
|------|------|
| 部门 | `/depts` |
| 员工 | `/emps` |
| 班级 | `/clazzs` |
| 学员 | `/students` |
| 课程 | `/courses` |
| 考勤 | `/attendances` |
| 成绩 | `/scores` |
| 缴费 | `/payments` |
| 就业 | `/employments` |
| 通知 | `/notices` |
| 违纪 | `/violations` |
| 日志 | `/logs` |

详细请求/响应格式见 [API接口文档.md](API接口文档.md)。

---

## 贡献指南

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

---

## 许可证

本项目基于 [MIT](LICENSE) 许可证开源。

---

<div align="center">

**🎉 感谢使用 慧学通！**

Made with ❤️ by 慧学通 Team

</div>
