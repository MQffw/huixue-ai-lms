# CLAUDE.md

本文件为在本仓库中工作的 Claude Code 提供上下文。

## 项目概览

慧学通（Huixuetong）智能教学管理系统：Spring Boot 3.5.13 + Vue 3 + Spring AI 1.0.0-M6。
以 AI Agent 为核心，提供教务数据查询、制度问答、统计分析与业务管理。

- 后端包名：`com.huixuetong`
- 构建产物：`target/huixuetong-lms-0.0.1-SNAPSHOT.jar`
- 前端源码：仓库内 `frontend/`（Vue 3 + Vite + Element Plus）

## 构建与运行

```bash
mvn clean package -DskipTests     # 构建
mvn spring-boot:run               # 开发运行
java -jar target/huixuetong-lms-0.0.1-SNAPSHOT.jar
mvn test                          # 测试（当前基线 121 个用例，全部通过）
```

## 技术栈

- Java 18、Spring Boot 3.5.13、Spring AI 1.0.0-M6、MyBatis 3.0.5
- MySQL 8.0（`127.0.0.1:3307`，库 `huixuetong`，账号 `huixuetong`）、Redis 8.x
- JWT（jjwt 0.9.1）、Guava RateLimiter、SpringDoc、Actuator
- 前端：Vue 3、Vite、Element Plus、ECharts

## 后端结构

```
src/main/java/com/huixuetong/
├── HuixuetongApplication.java
├── ai/
│   ├── orchestrator/AiOrchestratorService.java   # 统一编排：安全→意图→记忆→RAG→工具→SSE→持久化
│   ├── router/{Intent,IntentRouter}.java         # 6 类意图的规则路由
│   ├── tool/
│   │   ├── StudentTools.java       # 6 个 @Tool
│   │   ├── EmployeeTools.java      # 7 个
│   │   ├── ClazzTools.java         # 4 个
│   │   ├── CourseTools.java        # 4 个
│   │   ├── AffairsTools.java       # 14 个（考勤/成绩/违纪/缴费/就业）
│   │   ├── NoticeTools.java        # 3 个（SQL 兜底工具已禁用）
│   │   └── SqlQueryTools.java      # 只读 SQL 兜底（默认不注入）
│   ├── rag/                        # KnowledgeBaseService / KnowledgeRagAdvisor / SentenceSplitter
│   ├── memory/                     # RedisChatHistoryManager 等（部分未接线）
│   ├── advisor/                    # SafeGuard / QuestionAnswer / TokenTracking / Summarization
│   ├── cache/AiAnswerCache.java    # 上下文指纹缓存
│   ├── prompt/AiPromptBuilder.java # 按意图拼装 7 个 Prompt 模板
│   └── trace/AiTrace.java          # 全链路追踪（已接入：hxt_ai_trace + 工具日志）
├── config/
│   ├── ChatClientConfig.java       # 按注册表懒建 ChatClient
│   ├── model/                      # 模型注册表 + API Key AES-GCM 加密
│   └── AiHealthIndicator.java
├── controller/                     # 19 个 Controller
├── service/ + service/impl/        # 业务服务
├── mapper/                         # 22 个 MyBatis Mapper
├── pojo/                           # 实体与查询参数
├── interceptor/                    # TokenInterceptor / RateLimitInterceptor
├── aop/LogAspect.java              # 写操作审计到 emp_log
└── security/sql/                   # SQL 白名单/只读/关键字/子查询深度校验
```

## AI 调用链路

```
前端（携带 sessionId + token）
  → POST /ai/chat（SSE）
  → AiChatController
  → AiOrchestratorService
      1. SafeGuardAdvisor 正则注入检测
      2. IntentRouter 规则路由（DATA_STATS / DATA_QUERY / KNOWLEDGE_RAG / TEXT_GEN / CHAT / GREETING）
      3. 读 Redis 历史（ai:chat:session:{userId}:{sessionId}，40 条 / 24h）
      4. 按意图选 Tool Bean，Spring AI 执行 Function Calling
      5. 无工具意图走 token 级流式；工具意图先执行工具再分块推送
      6. 异步写 hxt_ai_chat_record、估算 Token 写 hxt_token_usage
```

## 数据库

业务表：`dept`、`emp`、`emp_expr`、`clazz`、`student`、`course`、`course_schedule`、
`attendance`、`exam`、`score`、`violation_log`、`payment`、`employment`、`notice`、`emp_log`。

AI 扩展表（`src/main/resources/db/schema-extend.sql`）：
`hxt_ai_chat_record`、`hxt_token_usage`、`hxt_ai_trace`、`hxt_tool_call_log`、
`hxt_knowledge_doc`、`hxt_knowledge_chunk`、`hxt_user_ai_profile`。

字段映射：
- gender：1=男，2=女
- degree：1=初中，2=高中，3=大专，4=本科，5=硕士，6=博士
- job：1=班主任，2=讲师，3=学工主管，4=教研主管，5=咨询师
- subject：1=Java，2=前端，3=大数据，4=Python，5=Go，6=嵌入式

## 主要接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/login` | 登录，返回 JWT |
| POST | `/ai/chat` | SSE 流式对话 |
| POST | `/ai/chat/sync` | 同步对话（调试） |
| GET | `/ai/history?sessionId=` | 会话历史 |
| GET/POST/DELETE | `/ai/models` | 模型注册表 |
| GET | `/ai-stats/overview\|tokens-24h\|model-distribution` | 用量统计 |
| GET/POST | `/attendance`、`/courses`、`/scores`、`/payments`、`/employments`、`/notices`、`/violations` | 业务 CRUD |
| GET/POST | `/knowledge/*` | 知识库上传/检索/文档管理 |

## 配置

- `application.yml`：数据源 `jdbc:mysql://127.0.0.1:3307/huixuetong`、Redis `localhost:6379`
- `config/models.json`：模型注册表（OpenAI 兼容协议），API Key 以 `enc:` AES-GCM 存储
- 环境变量：`DB_PASSWORD`、`MODEL_KEY_SECRET`、`JWT_SECRET`
- 旧变量 `deepseek-api` / `mimo-api` 仍可用于本机密钥注入

## 前端

```bash
cd frontend
npm install
npm run dev      # 5173，/api 代理到 8080
npm run build    # 产物 dist/
```

## 已知问题（改动前先看）

1. MySQL 5.5 已停用，所有配置统一指向 8.0（`127.0.0.1:3307`）；历史 5.5 仅作回滚，见 `docs/env/MySQL8.0安装与切换手册.md`。
2. 密码当前为明文比对（`EmpServiceImpl.login`），未使用 BCrypt。
3. `AnswerValidator`、`UserProfileMemory` 尚未接入主链路（`AiTrace`、`KnowledgeRagAdvisor` 已接入）。
4. 角色仅有 `ADMIN`，`@RequiresRole` 只校验登录状态。
5. 考勤写接口（含批量删除）、考试 CRUD 与页面已补齐；待办只剩预警、RBAC 与学生端。

## 约束

- 不提交 `target/`、`logs/`、`.iml`、`.env`、`config/models.json`
- 日志不得输出密码、手机号、API Key
- SQL 必须参数化，禁止字符串拼接
- 分页统一走 `PageQuery.of(page, pageSize)` → mapper `LIMIT/OFFSET` + `countPage` → `PageResult`；禁止 `findAll()` + `subList()`（不引入 PageHelper，保证离线构建）
- 修改 AI 链路后至少回归：登录、`/ai/chat/sync`、`/ai/chat`（SSE）、`/knowledge/upload`
