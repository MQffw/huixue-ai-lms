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

[API 文档](docs/API接口文档.md) · [问题反馈](https://github.com/MQffw/huixue-ai-lms/issues)

</div>

---

## 项目简介

慧学通是一款面向培训机构的智能教学管理系统，集成了基于 Spring AI 的 AI Agent 智能助手，支持：

- 自然语言查询/统计教务业务数据（部门、员工、班级、学员、考勤、成绩、缴费等）；
- 知识库 RAG 检索（制度、流程、通知文档）；
- 多模型动态接入与切换（模型注册表，新增模型零代码改动）；
- 多轮对话上下文消歧（"那教研部有谁"这类隔轮指代）；
- 工具未覆盖时走受控只读 SQL 兜底（四层沙箱防护）。

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
         │ Guard  │ │Memory │ │Intent  │    │   Tool   │   │  SQL兜底  │
         │安全拦截│ │上下文 │ │Router  │    │ Function │   │ 只读查询  │
         └────────┘ └───────┘ └────────┘    └──────────┘   └──────────┘
```

#### AI 核心能力（已实现）

- **混合流式输出** — 无工具意图（寒暄/文本生成/知识库）走 token 级真流式 SSE；工具意图先同步执行工具、再分块推送最终答案；
- **意图路由** — 规则优先分流（统计/查询/RAG/生成/闲聊），6 类域工具按需注入；
- **多轮上下文消歧** — 最近 2-3 轮对话（含助手回答）注入上下文，"男的分别有谁"可正确承接上一问的"学生"；
- **Function Calling** — Spring AI 原生工具调用，模型自动选择并执行业务工具；
- **只读 SQL 兜底** — 工具未覆盖的问题由模型生成只读 SQL，经四层沙箱（表白名单/只读/危险关键字/子查询深度）校验后执行；
- **知识库 RAG** — 文档上传 → 解析分块 → MySQL 检索 → Re-rank Top3（`embedding_json` 字段已预留向量化升级）；
- **模型注册表** — 模型配置外置 `config/models.json`，支持运行期热更新（`/ai/models` 接口 + 前端配置界面），新增模型零代码改动；
- **密钥加密存储** — API Key 支持环境变量占位或用户输入，明文保存时自动 AES-GCM 加密落盘，接口返回统一脱敏；
- **缓存上下文隔离** — AI 回答缓存 key 含会话 + 上下文指纹，不同上下文互不污染；仅缓存无工具意图的回答；
- **数据脱敏** — 流式链路中实时脱敏手机号/身份证号；
- **会话隔离** — 前端生成 sessionId，每个对话独立存储；
- **异步持久化** — 对话记录异步写入 MySQL，不阻塞响应；
- **AI 用量统计** — 24 小时 Token 趋势、模型占比、总览卡片。

#### 规划中 / 预留

- 语义记忆（LLM 摘要、用户画像）：`tlias_user_ai_profile` 表已建、接口预留，功能未接线；
- AI 全链路追踪（`tlias_ai_trace`）与工具调用日志（`tlias_tool_call_log`）：表已建，未启用；
- 故障自动降级：目前仅客户端创建时降级，请求失败未做二次切换。

### 安全特性

- **JWT Token 认证** — 无状态认证，过期前静默自动续期；
- **接口权限控制** — 拦截器 + `@RequiresRole` 注解 + AOP；
- **操作日志** — AOP 自动记录到 `emp_log`（不存对话内容）；
- **SQL 多层安全** — 白名单表、只允许 SELECT、DDL/DML 拦截、危险关键字拦截、子查询深度限制、SQL 长度限制；
- **密钥加密** — 模型 API Key 使用 AES-GCM 加密存储，加密密钥来自环境变量 `MODEL_KEY_SECRET`（未配置时使用开发默认密钥并启动告警）；
- **数据验证** — 前后端双重数据验证（JSR303）；
- **限流防护** — 普通接口 100 次/秒，AI 接口 10 次/秒。

---

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 18 | 编程语言 |
| Spring Boot | 3.5.13 | 应用框架 |
| Spring AI | 1.0.0-M6 | AI 框架（Function Calling + 流式 + Tool） |
| MyBatis | 3.5.19 | ORM 框架 |
| MySQL | 5.5+ | 数据库 |
| Redis | 5.x+ | 对话历史 + AI 回答缓存 |
| JWT | 0.9.1 | 认证方案 |

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

模型通过注册表动态管理，**以 `config/models.json` 为准**（首次启动自动从 `models.json.example` 生成）。默认内置示例：

| 标识 | 默认模型 | 提供商 |
|------|----------|--------|
| deepseek | deepseek-v4-pro | DeepSeek |
| mimo | mimo-v2.5-pro | 小米 |
| longcat | LongCat-2.0 | 美团 |

新增模型：登录后在 AI 页 → "⚙ AI 模型配置" 界面填写，或 `POST /ai/models`，保存即生效，无需重启。

---

## 快速开始

### 环境要求

- JDK 18+
- Maven 3.6+
- MySQL（5.5+ 或 8.x，含 `tlias` 库）
- Redis（3.x+）
- 至少一个 OpenAI 兼容模型的 API Key

### 1. 克隆项目

```bash
git clone https://github.com/MQffw/huixue-ai-lms.git
cd huixue-ai-lms
```

### 2. 数据库初始化

```sql
CREATE DATABASE tlias DEFAULT CHARACTER SET utf8;
USE tlias;
SOURCE src/main/resources/db/tlias_extend.sql;
```

> 说明：`tlias_extend.sql` 仅含 AI 模块扩展表；业务基础表（emp/dept/student 等）需从现有数据库导出导入，或按业务自行初始化。

### 3. 启动 Redis

```bash
redis-server
```

默认连接 `localhost:6379`，可在 `application.yml` 中修改。

### 4. 启动后端

```bash
mvn spring-boot:run
```

后端默认端口 `8080`。首次启动会自动生成 `config/models.json`（从 `models.json.example` 拷贝）。

### 5. 配置模型与密钥

登录系统后，打开 **AI 助手页 → "⚙ AI 模型配置"**，编辑模型并填写真实 API Key（保存时自动 AES 加密），或直接编辑 `config/models.json`：

```json
{
  "models": [
    {
      "type": "deepseek",
      "name": "DeepSeek V4 Pro",
      "baseUrl": "https://api.deepseek.com",
      "apiKey": "sk-xxx 或 ${ENV_NAME}",
      "model": "deepseek-v4-pro",
      "temperature": 0.3,
      "maxTokens": 1024,
      "enabled": true
    }
  ]
}
```

- 编辑模型时 **API Key 留空 = 不修改**；
- 生产环境务必设置环境变量 `MODEL_KEY_SECRET`（密钥加密），未设置会降级为开发默认密钥并打印告警；
- `config/models.json` 已被 .gitignore 忽略，不会提交密钥。

### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认端口 `5173`。

### 7. 访问

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:5173 |
| 后端 API / Swagger | http://localhost:8080/swagger-ui.html |

### 8. 测试账号

| 用户名 | 密码 | 权限 |
|--------|------|------|
| songjiang | 123456 | 管理员 |
| lujunyi | 123456 | 管理员 |

---

## 项目结构

```
huixue-ai-lms/
├── src/                              # Spring Boot 后端源码
│   ├── main/java/com/itheima/
│   │   ├── ai/
│   │   │   ├── orchestrator/         # AiOrchestratorService 统一编排层
│   │   │   ├── tool/                 # 6 个域 Tool 类 + SQL 兜底工具
│   │   │   ├── advisor/              # Advisor 链（SafeGuard/TokenTracking）
│   │   │   ├── memory/               # 上下文记忆（Redis 历史）
│   │   │   ├── rag/                  # RAG 知识检索
│   │   │   ├── prompt/               # Prompt 模板（7 个）
│   │   │   ├── router/               # IntentRouter 规则意图路由
│   │   │   └── cache/                # AI 回答缓存（上下文隔离）
│   │   ├── config/model/             # 模型注册表 + 密钥加密
│   │   ├── controller/               # 控制器（含 AiModelController）
│   │   ├── security/sql/             # SQL 四层沙箱
│   │   └── aop/                      # 操作日志切面
│   ├── main/resources/
│   │   ├── prompts/                  # 按意图拆分的 Prompt 模板
│   │   ├── db/tlias_extend.sql       # AI 模块扩展表
│   │   ├── models.json.example       # 模型注册表模板
│   │   └── application.yml           # 配置文件
│   └── test/java/                    # 单元测试
├── frontend/                         # Vue 3 前端源码
├── docs/                             # 设计文档（含 API 文档、方案合集）
├── config/models.json                # 模型注册表（运行时生成，gitignore）
├── pom.xml
└── README.md
```

---

## AI 模块扩展表

| 表名 | 用途 | 状态 |
|------|------|------|
| `tlias_knowledge_doc` | 知识库文档主表 | 使用中 |
| `tlias_knowledge_chunk` | 文档分块（RAG 核心） | 使用中（`embedding_json` 预留向量化） |
| `tlias_token_usage` | Token 统计 | 使用中 |
| `tlias_ai_chat_record` | 对话记录 | 使用中 |
| `tlias_user_ai_profile` | 用户画像 | 预留（功能未接线） |
| `tlias_ai_trace` | AI 全链路追踪 | 预留 |
| `tlias_tool_call_log` | 工具调用日志 | 预留 |

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
| POST | `/ai/cache/clear` | 清除当前用户 AI 缓存 |
| GET | `/ai/models` | 模型列表（apiKey 脱敏） |
| POST | `/ai/models` | 新增/修改模型（保存即生效） |
| DELETE | `/ai/models/{type}` | 删除模型 |

**对话请求体：**

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

详细请求/响应格式见 [API接口文档.md](docs/API接口文档.md)。

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