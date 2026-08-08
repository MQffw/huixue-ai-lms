# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Tlias 智能教学管理系统 - 基于 Spring Boot 3.5.13 + Vue 3 + Spring AI 的现代化培训管理系统。集成 AI Agent 智能助手，支持自然语言查询业务数据和日常对话。

## Build and Run Commands

```bash
# 构建项目
mvn clean package -DskipTests

# 运行项目
mvn spring-boot:run

# 打包后运行
java -jar target/tlias-web-management-0.0.1-SNAPSHOT.jar

# 运行所有测试
mvn test
```

## Architecture

### Technology Stack
- **Backend**: Java 18, Spring Boot 3.5.13, Spring AI 1.0.0-M6, MyBatis 3.0.5, MySQL 8.0, Redis 8.x
- **Frontend**: Vue 3, Vite, Element Plus (source: `C:\Users\mq\Desktop\vue-tlias-management`, dist: `front-dist/`)
- **AI**: Spring AI ChatClient + @Tool 模式，支持 DeepSeek / Mimo / LongCat 三种模型
- **Security**: JWT authentication, RBAC 权限控制, AOP 操作日志

### AI Agent 架构（核心）

```
前端(带sessionId) → AiChatController(userId+sessionId)
  → Redis 读历史(ai:chat:session:{userId}:{sessionId}, 24h TTL, 40条截断)
  → TliasAgentService.chat()
    → Spring AI ChatClient + @Tool 自动调用业务方法
  → 提取 <result> 标签（过滤 <thinking> 中间过程）
  → SSE 逐字输出（20ms/字，打字机效果）
  → Redis 同步更新历史
  → @Async 异步写入 tlias_ai_chat_record
  → AOP 自动记 emp_log（只记接口调用，不存对话内容）
```

### Key Components
```
src/main/java/com/itheima/
├── ai/                                 # AI 模块
│   ├── TliasAgentTools.java            # @Tool 工具类（13个方法，查询学员/员工/班级/部门）
│   └── RedisChatHistoryManager.java    # Redis 对话历史管理器
├── controller/
│   └── AiChatController.java           # AI 聊天（SSE + CompletableFuture 异步）
├── service/
│   ├── TliasAgentService.java          # Agent 服务接口
│   └── impl/
│       ├── TliasAgentServiceImpl.java  # Agent 实现（ChatClient + Redis + @Async MySQL）
│       ├── AiChatServiceImpl.java      # 旧实现（保留备用，SQL生成模式）
│       └── ...                         # 业务 Service
├── mapper/
│   ├── ChatRecordMapper.java           # 对话记录 Mapper
│   └── ...                             # 业务 Mapper
├── pojo/
│   ├── ChatRecord.java                 # 对话记录实体
│   └── ...                             # 业务实体
├── config/                             # 配置类
├── interceptor/                        # 拦截器（TokenInterceptor, RateLimitInterceptor）
├── aop/LogAspect.java                  # 操作日志（SSE响应记录为"SSE流式响应"，不存对话内容）
└── security/sql/                       # SQL 安全模块（旧模式保留备用）
```

### Database Tables

**业务表（AI 可查询）：**
- `dept` - 部门表 (id, name)
- `emp` - 员工表 (id, name, gender, phone, job, salary, dept_id)
- `clazz` - 班级表 (id, name, room, begin_date, end_date, master_id, subject)
- `student` - 学员表 (id, name, no, gender, degree, clazz_id, violation_count)

**系统表：**
- `emp_log` - 操作审计日志（AOP 自动记录）
- `tlias_ai_chat_record` - AI 对话记录（异步持久化）

**代码映射：**
- gender: 1=男, 2=女
- degree: 1=初中, 2=高中, 3=大专, 4=本科, 5=硕士, 6=博士
- job: 1=班主任, 2=讲师, 3=学工主管, 4=教研主管, 5=咨询师
- subject: 1=Java, 2=前端, 3=大数据, 4=Python, 5=Go, 6=嵌入式

### API Endpoints
- `POST /login` - 登录（获取JWT token）
- `POST /ai/chat` - AI聊天（SSE流式，支持 sessionId）
- `POST /ai/chat/sync` - AI同步聊天（测试用）
- `GET/POST /depts` - 部门管理
- `GET/POST /emps` - 员工管理
- `GET/POST /clazzs` - 班级管理
- `GET/POST /students` - 学员管理
- `GET /report/*` - 数据统计

### Configuration
- `application.yml` - 数据库、Redis、AI 多模型配置
- AI API Key: 环境变量 `deepseek-api` / `mimo-api` / `longcat-api`
- Redis: 默认 `localhost:6379`，无密码
- MySQL: 默认 `localhost:3306/tlias`，root/123456

## AI Prompt 设计（RTCF 框架）

System Prompt 遵循 Role-Task-Context-Format 结构：
- **Role**: Tlias智能教务系统AI助手
- **Task**: 三种行为（业务查询/日常回答/意图确认）
- **Context**: 系统数据范围、可用工具、代码映射
- **Format**: 纯文本，`<result>` 包裹最终答案

AI 响应结构：`<thinking>` 中间推理 + `<result>` 最终答案，服务端只取 `<result>`。

Few-Shot 示例 3 个：业务查询、日常回答、意图确认。

## Development Notes

### 前端开发
- Vue 3 源码在 `C:\Users\mq\Desktop\vue-tlias-management`
- 修改后执行 `npm run build`，将 `dist/` 复制到 `front-dist/dist/`
- 前端自动生成 sessionId（crypto.randomUUID），每次请求带上

### 后端开发
- `@EnableAsync` 已启用，`@Async` 方法异步执行
- Redis 使用 `StringRedisTemplate`，JSON 序列化
- ChatClient 按模型缓存（ConcurrentHashMap），避免重复创建
- `<result>` 标签提取逻辑在 `TliasAgentServiceImpl.extractResult()`

### 调试 AI 功能
1. 使用 `/ai/chat/sync` 接口测试同步响应
2. 检查日志中的 `Agent 原始响应` 和 `Agent 最终回答`
3. 验证 Redis: `redis-cli LRANGE ai:chat:session:{userId}:{sessionId} 0 -1`
4. 验证 MySQL: `SELECT * FROM tlias_ai_chat_record ORDER BY id DESC LIMIT 10`

## Security Best Practices

### 必须遵守
1. **参数校验**: Controller 使用 `@Valid` + JSR303
2. **权限控制**: JWT 拦截器自动校验
3. **事务管理**: `@Transactional(rollbackFor = Exception.class)`
4. **日志脱敏**: 不打印密码、手机号等敏感信息
5. **异步持久化**: 对话记录用 `@Async`，不阻塞 SSE 响应

### 禁止事项
- 禁止在日志中打印密码、手机号等敏感信息
- 禁止在代码中硬编码密钥、密码
- 禁止直接拼接SQL（必须使用MyBatis参数化）
- 禁止将 `.iml`、`target/`、`logs/` 等提交到Git
