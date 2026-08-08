# Tlias AI Agent 深度优化开发方案（合集）

> 本文档由《方案2（终版）》与《AI_Agent深度优化开发方案》合并而成，保留全部内容：第一部分为实施状态与总体说明，第二部分为分阶段详细设计（含数据库设计、简历描述、面试问答）。

---

# 第一部分：方案2（终版）——实施状态与总体说明

Tlias AI Agent 深度优化开发方案（终版）
基于 Spring Boot 3.5 + Spring AI 1.0 M6 + Vue 3 的智能教务管理系统 从"CRUD + AI 问答助手"演进为"以 AI Agent 为核心的智能教务中台" 定位：解决业务真实痛点 + 提升简历含金量 + 参考高星项目做法

> **📌 实施状态更新（2026-07-05）**
>
> | Phase | 内容 | 状态 | 关键交付 |
> |-------|------|------|---------|
> | **P0** | 引入 `AiOrchestratorService`，统一编排层 | ✅ 已完成 | Guard → Memory → IntentRoute → Tool/RAG → Validator → SSE |
> | **P0** | Intent Router（规则优先分流 6 类意图） | ✅ 已完成 | 6 类域工具按需注入，不再一次性发 40+ Tool |
> | **P0** | Tool 分组（6 个域 Tool 类）+ Prompt 模块化 | ✅ 已完成 | StudentTools/EmployeeTools/ClazzTools/CourseTools/AffairsTools/NoticeTools |
> | **P0** | SSE 真流式（Spring AI 原生 Flux\<String\>） | ✅ 已完成 | 移除 Thread.sleep 假流式，首字延迟 < 2s |
> | **P1** | 真正的 RAG（分块 + FULLTEXT 检索 + Re-rank） | ✅ 已完成 | KnowledgeService + SentenceSplitter + 预留 VectorStore 接口 |
> | **P1** | 4 层语义记忆（短期 + 摘要 + 用户画像） | ✅ 已完成 | ShortTermMemory / SummaryMemory / UserProfileMemory |
> | **P1** | Answer Validator（数字一致性校验） | ✅ 已完成 | AnswerValidator 规则优先校验 |
> | **P2** | Tool 调用全链路追踪（AiTrace 落表） | ✅ 已完成 | tlias_ai_trace + tlias_tool_call_log + tlias_token_usage |
> | **P2** | 工具调用 AOP 统计 | ✅ 已完成 | ToolCallAspect + ToolCallStatsPostProcessor (CGLIB) |
> | **P2** | Prompt 重构（7 个模板文件按意图组合） | ✅ 已完成 | AiPromptBuilder + prompts/*.txt |
> | **P2** | 受控 Text-to-SQL（只读 + SQL 校验） | ⚠️ 基础设施就绪 | SecureSqlExecutor + SqlSecurityValidator 已复用 |
> | **P3** | MCP Server 集成 | 📋 规划中 | TliasMcpServerConfig 已预留 39 个工具 |
>
> **已实现文件清单**：`orchestrator/`, `tool/`, `advisor/`, `memory/`, `rag/`, `validator/`, `trace/`, `prompt/`, `IntentRouter.java`, `Intent.java`, `DataMasker.java`, `SentenceSplitter.java`, `ToolCallStatsPostProcessor.java`, `SpringContextUtil.java`
>
> **已删除旧系统**：`AiChatServiceImpl.java`（SQL 模式）, `AiChatService.java`, `TliasAgentServiceImpl.java`（[[TOOL]] 反射模式）

---

一、现状分析与痛点诊断

> 以下为重构前现状分析，重构后已解决 ⬇️ 中标记的痛点。

1.1 当前已实现能力（基于实际代码盘点）
模块	实现位置	现状
对话入口	AiChatController → TliasAgentServiceImpl	REST + SseEmitter
模型接入	三模型切换（DeepSeek/Mimo/LongCat），OpenAI 协议兼容	Spring AI 1.0.0-M6
业务工具	TliasAgentTools 16 个 @Tool 方法	仅本地调用，查 MySQL
对话历史	RedisChatHistoryManager	Redis List，40 条，24h TTL，全量注入 prompt
流式输出	Thread.sleep(20) 逐字推送	假流式，首字延迟=完整生成时间
响应解析	extractResult() 解析 <result> 标签	M6 时代补丁
工具调用记录	extractToolCalls() 从 <thinking> 正则猜测	不准确
持久化	@Async 写入 tlias_ai_chat_record	仅存对话原文
SQL 安全	SecureSqlExecutor + SqlSecurityValidator	仅限 executeQuery 兜底工具
鉴权	JWT + TokenInterceptor + 权限注解	已完善
限流	RateLimiterConfig + RateLimitInterceptor	已完善
1.2 核心痛点（按面试杀伤力排序，✅ 表示重构后已解决）
痛点	当前实现	影响	面试杀伤力	状态
AI 只会查结构化数据	16 个 @Tool 全是 SQL 查询	用户问"退费流程""请假制度"只能靠模型编（幻觉）	🔴 高	✅ RAG + Notice 工具
Thread.sleep(20) 假流式	Controller 逐字推送	首字延迟=完整生成时间，非真实流式	🔴 面试必问	✅ Spring AI 流式
对话历史全量注入	40 条原文拼进 system prompt	token 浪费、无语义关联、长对话溢出	🟡 中	✅ 4 层记忆 + IntentRouter
手动构建 ChatClient	ConcurrentHashMap 缓存，无 Advisor 链	能力无法声明式挂载	🟡 中	✅ Advisor 链（SafeGuard/QAToken/...）
<result> 标签解析	extractResult() 正则提取	M6 时代补丁，GA 不需要	🟡 中	✅ 已移除
工具调用靠正则猜	extractToolCalls() 解析 <thinking>	不准确，无法做调用链分析	🟢 低	✅ Spring AI Function Calling
无 Prompt Injection 防御	全项目无	任何人可"忽略之前指令"	🟡 中	✅ SafeGuardAdvisor 12 条正则
数据库只有静态表	无考勤/成绩/违纪明细/缴费/就业	AI 无法回答高频业务问题	🔴 高	✅ 新增 12 张表 + 14 个 Tool
1.3 数据库现状缺口（✅ 已补齐）
重构前仅 7 张表：clazz、dept、emp、emp_expr、emp_log、student、tlias_ai_chat_record。

**重构后新增 12 张业务表 + 5 张 AI 统计表**：

新增业务表（12 张）
course	course_schedule	attendance	exam	score
violation_log	payment	employment	notice	emp_expr（已有）
tlias_ai_chat_record（已有）	course_schedule（已有）

新增 AI 统计表（5 张）
tlias_knowledge_doc	tlias_knowledge_chunk	tlias_ai_trace	tlias_tool_call_log	tlias_token_usage
tlias_user_ai_profile

现在 AI 可以回答："今天谁没来上课""某学员考了多少分""Java1班就业率多少""请假流程是什么"等高频业务问题。

二、总体架构设计（✅ 已按本架构实施）
2.1 架构全景（重构后实际实现）

Plain Text

┌─────────────────────────────────────────────────────────────────────────┐
│                        前端 (Vue 3 + SSE)                               │
└───────────────────────────┬─────────────────────────────────────────────┘
│
POST /ai/chat (原生 Flux 流式)  /  POST /ai/chat/sync (同步)
│
┌───────────────────────────▼─────────────────────────────────────────────┐
│                     AiChatController                                    │
│              (原生流式 + Flux<String> + SseEmitter)                       │
│              └── 流式脱敏 .map(this::maskSensitiveData)                  │
└───────────────────────────┬─────────────────────────────────────────────┘
│
┌───────────────────────────▼─────────────────────────────────────────────┐
│                    AiOrchestratorService (P0)                           │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ 1. SafeGuardAdvisor.check() ──→ Prompt Injection 拦截           │    │
│  └──────────────────────────────┬──────────────────────────────────┘    │
│                                 ▼                                       │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ 2. MemoryLoad ──→ ShortTermMemory + SummaryMemory + UserProfile │    │
│  └──────────────────────────────┬──────────────────────────────────┘    │
│                                 ▼                                       │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ 3. IntentRouter.route() ──→ 6 类意图分流，选对应 Tool Bean      │    │
│  │    DATA_STATS / DATA_QUERY / KNOWLEDGE_RAG / TEXT_GEN / CHAT    │    │
│  └──────────────────────────────┬──────────────────────────────────┘    │
│                                 ▼                                       │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ 4. ChatClient.prompt().tools(域Tool).stream().content()         │    │
│  │    → Spring AI 原生 Function Calling（模型自动选工具）           │    │
│  │    → ToolCallAspect AOP 自动记录 tool_name/duration/success     │    │
│  └──────────────────────────────┬──────────────────────────────────┘    │
│                                 ▼                                       │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ 5. AnswerValidator ──→ 数字一致性校验                           │    │
│  │ 6. AiTrace 落表 ──→ intent / tool_calls / latency / error       │    │
│  │ 7. Cache Aside ──→ AiAnswerCache + Redis 历史                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
│                                    │
┌────────▼────────┐              ┌────────────▼────────────┐
│  Redis (缓存)    │              │     MySQL (业务数据)    │
│  - 对话历史 List  │              │  原7张表 + 新增12张     │
│  - 短期记忆       │              │  + AI 统计 5 张         │
│  - 摘要记忆       │              │  (FULLTEXT 检索 RAG)    │
│  - 回答缓存       │              └─────────────────────────┘
└──────────────────┘

预留升级路径：Redis Stack VectorStore（替换 MySQL FULLTEXT）→ 真正的向量检索
2.2 知识获取双路架构（✅ 已实现）
结构化数据路径（Function Calling）：用户问"Java 班有多少人" → IntentRouter → DATA_STATS → 注入 StudentTools.countStudentsByDegree → 模型调用 @Tool 查 MySQL
非结构化知识路径（RAG）：用户问"退费流程是什么" → IntentRouter → KNOWLEDGE_RAG → KnowledgeBaseService.search() → MySQL FULLTEXT + Re-rank Top3 → 注入文档片段 → 模型基于文档回答
路径切换：由 IntentRouter 规则优先决定（不调 LLM），6 类意图对应 6 种 Tool 注入策略。检索结果为空时，System Prompt 引导模型如实告知
2.3 三层语义记忆架构

Plain Text

对话记忆 = 近期原文 (Redis List, 最近10条)        ← 短期连贯性
+ 语义召回 (向量库, 相关5条)              ← 跨会话关联
+ 历史摘要 (LLM压缩, 1段200字)            ← 释放上下文窗口
三者合并后注入上下文，token 消耗从"40条原文约4000 token"降到"10条原文+5条召回+1段摘要约1500 token"，降低 60%+。

三、数据库详细结构设计
3.1 现有表（7张，保留不动）
表名	作用	说明
dept	部门	组织架构，6个部门
emp	员工	教职工信息，含脱敏字段
emp_expr	工作经历	员工履历
emp_log	操作日志	AOP 审计日志
clazz	班级	班级信息，6个班级
student	学员	学员信息，含违纪汇总字段
tlias_ai_chat_record	AI对话记录	已有，保留
3.2 新增表：A 类 - 教务业务表（9张）
3.2.1 course 课程表
解决痛点： 当前 clazz 表只有 subject 字段（1-6 枚举），无法表达"Java 基础""Spring Boot""MySQL"等具体课程。

字段	类型	说明
id	int unsigned PK	课程ID
name	varchar(50) UNIQUE	课程名称
subject	tinyint	学科: 1:java 2:前端 3:大数据 4:Python 5:Go 6:嵌入式
hours	int	课时数
description	varchar(500)	课程简介
create_time / update_time	datetime	时间戳
索引： 主键 + name 唯一索引 + subject 普通索引

预置数据： Java基础、MySQL数据库、Spring Boot、Vue3前端开发、Hadoop大数据、Python基础 共6门课程

对 AI 的价值： AI 可回答"Java 方向学哪些课程""Spring Boot 课多少课时"

3.2.2 course_schedule 排课表
解决痛点： 培训班每天有课表，当前完全无法表达"今天哪个班上什么课"。

字段	类型	说明
id	int unsigned PK	排课ID
clazz_id	int unsigned	班级ID（关联 clazz）
course_id	int unsigned	课程ID（关联 course）
teacher_id	int unsigned	授课教师（关联 emp）
class_date	date	上课日期
start_time / end_time	time	开始/结束时间
room	varchar(20)	教室
create_time	datetime	创建时间
索引： 主键 + (clazz_id, class_date) 联合索引 + (teacher_id, class_date) 联合索引

对 AI 的价值： AI 可回答"今天什么课""谁上课""在哪个教室"

3.2.3 attendance 考勤记录表
解决痛点： 培训班最核心的日常管理，当前完全缺失。AI 无法回答"今天谁没来上课"。

字段	类型	说明
id	int unsigned PK	考勤ID
student_id	int unsigned	学员ID（关联 student）
clazz_id	int unsigned	班级ID（关联 clazz）
attend_date	date	考勤日期
status	tinyint	状态: 1:正常 2:迟到 3:早退 4:请假 5:旷课
remark	varchar(200)	备注（如"迟到10分钟""病假"）
record_emp_id	int unsigned	记录人（关联 emp）
create_time	datetime	创建时间
索引： 主键 + (student_id, attend_date) 唯一索引（一人一天一条）+ (clazz_id, attend_date) 联合索引 + status 普通索引

对 AI 的价值： AI 可回答"段誉今天来了吗""Java 班今天谁旷课了""本月出勤率多少"

3.2.4 exam 考试表
解决痛点： 培训班核心考核业务，当前完全缺失。

字段	类型	说明
id	int unsigned PK	考试ID
name	varchar(50)	考试名称（如"Java基础阶段测试"）
clazz_id	int unsigned	班级ID（关联 clazz）
course_id	int unsigned	课程ID（关联 course）
exam_date	date	考试日期
full_score	int	满分（默认100）
pass_score	int	及格分（默认60）
create_time	datetime	创建时间
索引： 主键 + clazz_id 索引 + course_id 索引

3.2.5 score 成绩表
解决痛点： 无成绩表，AI 无法回答"某学员考了多少分""班级平均分"。

字段	类型	说明
id	int unsigned PK	成绩ID
exam_id	int unsigned	考试ID（关联 exam）
student_id	int unsigned	学员ID（关联 student）
score	decimal(5,1)	分数
rank	int unsigned	排名
remark	varchar(200)	备注（如"不及格,需补考"）
create_time	datetime	创建时间
索引： 主键 + (exam_id, student_id) 唯一索引（一次考试一个学生一条成绩）+ student_id 索引

对 AI 的价值： AI 可回答"段誉考了多少分""这次考试平均分多少""谁不及格"

3.2.6 violation_log 违纪记录明细表
解决痛点： 当前 student 表只有 violation_count 和 violation_score 两个汇总字段，查不到"什么时候、因为什么、扣了几分、谁处理的"。

字段	类型	说明
id	int unsigned PK	违纪ID
student_id	int unsigned	学员ID（关联 student）
violation_type	varchar(50)	违纪类型: 迟到/旷课/作弊/打架/其他
violation_date	date	违纪日期
deduct_score	tinyint	扣分
description	varchar(500)	违纪描述
handler_id	int unsigned	处理人（关联 emp）
create_time	datetime	创建时间
索引： 主键 + (student_id, violation_date) 联合索引 + violation_type 索引

对 AI 的价值： AI 可回答"段誉最近有什么违纪""本月违纪次数最多的是谁""作弊扣几分"

3.2.7 payment 缴费记录表
解决痛点： 培训班核心财务业务，AI 无法回答"某学员交了多少学费""还欠多少"。

字段	类型	说明
id	int unsigned PK	缴费ID
student_id	int unsigned	学员ID（关联 student）
amount	decimal(10,2)	缴费金额
payment_type	varchar(20)	费用类型: 学费/住宿费/教材费/押金
payment_method	varchar(20)	缴费方式: 现金/微信/支付宝/银行转账
payment_date	date	缴费日期
status	tinyint	状态: 1:已缴费 2:待确认 3:已退款
operator_id	int unsigned	操作人（关联 emp）
remark	varchar(200)	备注
create_time	datetime	创建时间
索引： 主键 + student_id 索引 + payment_date 索引

对 AI 的价值： AI 可回答"段誉学费交了吗""谁还没交学费""本月收了多少学费"

3.2.8 employment 就业记录表
解决痛点： 培训班最终成果跟踪，AI 无法回答"某班就业率多少""薪资最高多少"。

字段	类型	说明
id	int unsigned PK	就业ID
student_id	int unsigned	学员ID（关联 student）
clazz_id	int unsigned	班级ID（关联 clazz）
company	varchar(50)	就业公司
position	varchar(50)	职位
salary	int unsigned	入职薪资（元/月）
city	varchar(20)	就业城市
employment_date	date	就业日期
status	tinyint	状态: 1:在职 2:离职 3:试用期
create_time	datetime	创建时间
索引： 主键 + student_id 索引 + clazz_id 索引

对 AI 的价值： AI 可回答"Java 班就业率多少""薪资最高多少""谁去了阿里"

3.2.9 notice 通知公告表
解决痛点： 学校通知、制度文档当前无存储，可同时作为 RAG 知识库的文档来源。

字段	类型	说明
id	int unsigned PK	公告ID
title	varchar(100)	标题
content	text	内容
type	tinyint	类型: 1:通知 2:公告 3:制度
target_audience	varchar(50)	目标受众: 全体/教师/学员/某班级
publish_emp_id	int unsigned	发布人（关联 emp）
publish_time	datetime	发布时间
is_top	tinyint	是否置顶: 0:否 1:是
create_time	datetime	创建时间
索引： 主键 + (type, publish_time) 联合索引

对 AI 的价值： AI 可回答"有什么新通知""退费流程是什么"（制度类公告同步导入知识库）

3.3 新增表：B 类 - AI Agent 优化表（3张）
3.3.1 tlias_knowledge_doc 知识库文档管理表
用途： 配合 RAG 方案，记录导入的知识库文档元数据，支持按分类检索、去重、状态管理。

字段	类型	说明
id	bigint PK	主键ID
file_name	varchar(255)	原始文件名
category	varchar(50)	分类: policy/course/faq/teacher/student
chunk_count	int	分块数量
status	varchar(20)	状态: PROCESSING/READY/FAILED
file_path	varchar(500)	文件存储路径
create_time	datetime	创建时间（默认当前时间）
update_time	datetime	修改时间（自动更新）
索引： 主键 + category 索引

设计要点：

导入文档前按 category + source 清理旧向量，避免重复存储
status 字段标记文档处理状态，支持异步导入
chunk_count 记录分块数量，便于向量数据量统计
3.3.2 tlias_tool_call_log AI 工具调用日志表
用途： 替代当前从 <thinking> 标签正则猜测工具调用的方式，使用 Spring AI 结构化 ToolCall 元数据准确记录。

字段	类型	说明
id	bigint PK	主键ID
user_id	int	调用者ID
session_id	varchar(100)	会话ID
tool_name	varchar(100)	工具方法名
tool_args	text	调用参数（JSON）
result	text	返回结果（截断）
duration_ms	bigint	执行耗时（毫秒）
success	tinyint(1)	1成功 0失败
error_msg	varchar(500)	失败原因
call_time	datetime	调用时间（默认当前时间）
索引： 主键 + (user_id, call_time) 联合索引 + tool_name 索引

设计要点：

从 Spring AI 的 AdvisedResponse 结构化元数据提取，不再正则猜测
result 截断存储，避免单条记录过大
duration_ms 支持工具性能分析
支持按用户、按工具、按时间维度统计调用频次和成功率
3.3.3 tlias_token_usage AI Token 用量统计表
用途： 按用户+模型+日期维度统计 token 消耗，支持成本治理和超额告警。

字段	类型	说明
id	bigint PK	主键ID
user_id	int	用户ID
model_name	varchar(50)	模型名: deepseek/mimo/longcat
prompt_tokens	int	输入token
completion_tokens	int	输出token
total_tokens	int	总token
session_id	varchar(100)	会话ID
record_time	datetime	记录时间（默认当前时间）
索引： 主键 + (user_id, record_time) 联合索引 + model_name 索引

设计要点：

从 AdvisedResponse 的 Usage 元数据提取
支持按用户统计日/周/月 token 消耗
支持按模型统计成本（不同模型单价不同）
超过阈值（如每日50000 token）触发告警
3.4 新增表对 AI Agent 的价值总览
补齐这12张表后，TliasAgentTools 可新增大量 @Tool 方法，AI 能力从"查人查数"扩展到"管课管考管费管就业"：

新增 @Tool	对应表	示例问题
getStudentAttendance	attendance	"段誉今天来了吗"
getClassAttendance	attendance	"Java 班今天谁旷课了"
getStudentScores	score + exam	"段誉考了多少分"
getClassAvgScore	score	"这次考试平均分多少"
getStudentViolations	violation_log	"段誉最近有什么违纪"
getStudentPaymentStatus	payment	"段誉学费交了吗"
getClassEmploymentRate	employment	"Java 班就业率多少"
getTodaySchedule	course_schedule	"今天什么课"
getCourseList	course	"Java 方向学哪些课程"
searchNotice	notice	"有什么新通知"
四、分阶段开发计划
Phase 1：数据库扩展 + 业务工具补齐（基础）
目标： 补齐教务核心业务表，新增对应 @Tool 方法

新增表：

A 类 9 张表：course、course_schedule、attendance、exam、score、violation_log、payment、employment、notice
新增 @Tool 方法（TliasAgentTools 扩展）：

考勤类：getStudentAttendance、getClassAttendance、getAttendanceRate
成绩类：getStudentScores、getClassAvgScore、getExamRanking
违纪类：getStudentViolations、getRecentViolations
缴费类：getStudentPaymentStatus、getClassPaymentRate
就业类：getClassEmploymentRate、getStudentEmployment
课程类：getCourseList、getTodaySchedule
公告类：searchNotice
验收标准：

用户问"段誉今天来了吗"时，AI 调用 getStudentAttendance 回答
用户问"Java 班就业率多少"时，AI 调用 getClassEmploymentRate 回答
Phase 2：Advisor 架构重构 + 原生流式（核心修复）
目标： 升级 Spring AI M6 → GA，废弃手动构建 ChatClient、Thread.sleep、<result> 解析

改造内容：

pom.xml 升级

spring-ai.version 从 1.0.0-M6 改为 1.0.0
新增 spring-ai-redis-store-spring-boot-starter、spring-ai-tika-document-reader
移除 spring-milestones 仓库（GA 版在 Maven 中央仓库）
新增 ChatClientConfig 配置类

声明式构建 ChatClient，替代 TliasAgentServiceImpl 中的手动构建
注册 Advisor 链：SafeGuard → Summarization → QuestionAnswer → ChatMemory → TokenTracking → ToolCallLog
System Prompt 精简：移除 Few-Shot 中的 <result> 标签示例
改造 AiChatController

废弃 Thread.sleep(20) 逐字推送
改用 chatClient.prompt().stream().content() 原生 Flux 流式
在 Flux 链中用 .map(this::maskSensitiveData) 做流式脱敏
移除 extractResult() 调用
精简 TliasAgentServiceImpl

移除 getChatClient()、createChatModel()、buildSystemPromptWithHistory()、extractResult()、extractToolCalls()
改为直接调用 ChatClient（由 ChatClientConfig 注入）
验收标准：

首字延迟 < 2 秒（模型输出第一个 token 即推送）
代码中不再出现 Thread.sleep、<result>、<thinking> 字样
新增能力只需添加一个 Advisor 组件，无需改核心调用链
Phase 3：RAG 知识库（核心突破）
目标： 让 AI 能回答"退费流程""请假制度"等非结构化知识问题

技术选型：

向量库：Redis Stack（项目已用 Redis，零运维成本；HNSW 索引，万级文档够用）
Embedding 模型：text-embedding-3-small（1536维，DeepSeek 无 embedding 模型，用 OpenAI 兼容端点）
文档解析：Apache Tika（支持 PDF/Word/TXT/Markdown）
分块策略：TokenTextSplitter（500 token/块，100 token 重叠）
新增模块：

KnowledgeBaseService：文档导入（Tika解析→分块→注入元数据→向量化存储）、语义检索（支持按 category 过滤）
KnowledgeController：管理接口（上传/检索/清理）
去重策略：导入前按 category + source 清理旧向量，避免重复存储
知识库分类设计：

分类	内容	示例问题
policy	退费流程、请假制度、违纪处分条例	"退费流程是什么"
course	课程大纲、考核标准、教学计划	"Java 课程的考核标准"
faq	常见问题解答	"怎么补办学生证"
teacher	教师行为规范、考勤制度	"教师的考勤要求"
student	学员管理制度	"违纪扣分规则"
与 QuestionAnswerAdvisor 集成：

promptTemplate 必须包含 {query} 占位符（修复审查报告缺陷2）
检索结果为空时，System Prompt 引导模型调用工具或如实告知
验收标准：

管理员可上传 PDF/Word 文档，系统自动分块向量化存入知识库
用户问"退费流程"时，AI 从知识库检索真实文档回答，不编造
用户问"Java 班人数"时，AI 调用 @Tool 查 MySQL，不走 RAG
Phase 4：语义对话记忆（展示深度）
目标： 替代 Redis List 全量注入，实现三层语义记忆 + 摘要压缩

新增模块：

SemanticChatMemory（实现 ChatMemory 接口）
add()：每条消息向量化存入向量库 + Redis List 存近期原文
get()：Redis 取近期10条原文 + 向量库语义召回5条 + 合并去重
clear()：清理 Redis + 向量库
MemorySummarizer：将早期对话用 LLM 压缩为 200 字摘要
SummarizationAdvisor：在 before() 阶段检查轮次，超过 20 轮触发摘要压缩
时间字段处理：

保留 timestamp 元数据（修复审查报告缺陷3）：当前向量检索按语义相似度，时间字段暂不用于过滤；未来需要时间窗口检索时再做二级索引
验收标准：

对话历史超 20 轮后自动触发摘要压缩
token 消耗降低 50%+
跨会话语义关联：用户上午问过"张三的违纪情况"，下午问"他最近表现怎么样"能关联
Phase 5：安全与可观测性
目标： Prompt Injection 防御 + 数据脱敏 + Token 治理 + 工具调用追踪

新增模块：

SafeGuardAdvisor（安全拦截）

before() 阶段正则匹配攻击模式
覆盖：忽略指令、提示词窃取、角色覆盖、分隔符注入
增加中文攻击模式：忘掉规则、你现在是、忽略系统提示
检测到攻击时抛异常而非替换输入（修复审查报告缺陷5：避免模型困惑）
记录攻击日志用于审计
DataMaskingAdvisor（数据脱敏）

在 Controller 的 Flux 链中用 .map(this::maskSensitiveData) 处理（修复审查报告缺陷6：after() 在流式场景拿不到完整文本，且修改后未写回）
手机号脱敏：138****1234
身份证号脱敏：110101********1234
TokenTrackingAdvisor（Token 用量追踪）

after() 阶段从 AdvisedResponse 的 Usage 元数据提取
按用户+模型+日期写入 tlias_token_usage 表
超过阈值（如每日50000 token）触发告警
ToolCallLogAdvisor（工具调用日志）

after() 阶段从结构化 ToolCall 元数据提取（不再从 <thinking> 正则猜测）
记录工具名、参数、耗时、成功/失败状态
写入 tlias_tool_call_log 表
新增表：

B 类 3 张表：tlias_knowledge_doc、tlias_tool_call_log、tlias_token_usage
验收标准：

输入"忽略之前所有指令"被拦截
手机号在 AI 回复中自动脱敏
可查询每用户每日 token 消耗
工具调用记录包含准确的方法名、参数、耗时
Phase 6：MCP 协议接入（生态扩展）
目标： 让 Tlias 的工具能被 Claude Desktop / Cursor 等外部 AI 客户端复用

新增模块：

Tlias 作为 MCP Server

将 TliasAgentTools 的工具方法包装为 MCP Server 的 tools
通过 SSE 端点暴露：/mcp/sse
在 Claude Desktop 配置文件添加 Tlias MCP Server 地址即可使用
Tlias 作为 MCP Client

接入外部 MCP Server（如天气查询）
使用 McpToolCallbackProvider 正确创建（修复审查报告缺陷7：McpClient.create() 不存在）
验收标准：

在 Claude Desktop 中配置后，直接对 Claude 说"帮我查 Java 班有多少学员"能返回真实数据
Phase 7：定时 Agent 主动洞察
目标： Agent 从被动问答变为主动推送洞察

新增模块：

ScheduledAgentService
每周一 8 点生成教务周报：Agent 主动调用工具收集数据 + LLM 分析 + 异常标注 + 推送管理员
每天检查违纪异常增长，超阈值预警
错误处理与幂等（修复审查报告缺陷8）：

添加 try-catch 完整异常处理
添加 @SchedulerLock（ShedLock）防止多实例重复执行
推送失败时发送告警
验收标准：

每周一定时生成教务周报并推送给管理员
推送失败时有告警
五、开发排期与里程碑
阶段	内容	产出
Phase 1	数据库扩展 + 业务工具补齐	12张新表 + 10+ 新 @Tool 方法
Phase 2	Advisor 重构 + 原生流式	ChatClientConfig + Advisor 链 + Controller 原生 SSE
Phase 3	RAG 知识库	KnowledgeBaseService + QuestionAnswerAdvisor + 知识库管理接口
Phase 4	语义对话记忆	SemanticChatMemory + MemorySummarizer + SummarizationAdvisor
Phase 5	安全与可观测性	SafeGuardAdvisor + DataMaskingAdvisor + TokenTrackingAdvisor + ToolCallLogAdvisor
Phase 6	MCP 协议	TliasMcpTools + MCP Server 配置 + Claude Desktop 联调
Phase 7	定时 Agent	ScheduledAgentService + 周报生成 + 违纪预警
实施顺序建议（按"面试加分值 ÷ 实现成本"排序）
顺序	模块	理由
1	Phase 1 数据库扩展	基础数据，所有后续模块依赖
2	Phase 2 Advisor + 原生流式	核心架构，修复面试必问的 sleep 问题
3	Phase 5 SafeGuardAdvisor	安全优先，实现成本低
4	Phase 3 RAG 知识库	核心突破，直接解决幻觉痛点
5	Phase 4 语义对话记忆	展示深度，token 数据有说服力
6	Phase 5 Token + 工具调用日志	可观测性，成本治理
7	Phase 6 MCP 协议	差异化亮点，适合简历末尾
8	Phase 7 定时 Agent	锦上添花
六、配置文件更新要点
6.1 pom.xml
spring-ai.version：1.0.0-M6 → 1.0.0
移除 spring-milestones 仓库
新增：spring-ai-redis-store-spring-boot-starter、spring-ai-tika-document-reader、spring-ai-mcp-server-spring-boot-starter
6.2 application.yml 新增
spring.ai.vectorstore.redis：index、prefix、initialize-schema
spring.ai.embedding.openai：api-key、base-url、model（text-embedding-3-small）
spring.ai.mcp.server：name、version、sse-endpoint
七、面试核心问答准备
Q: 你的 Agent 怎么决定走 RAG 还是 Function Calling？

A: 我不需要手动判断。Spring AI 的 QuestionAnswerAdvisor 会在每次请求时自动执行向量检索，检索结果作为上下文注入。同时 @Tool 方法注册在 ChatClient 上。模型拿到"用户问题 + 知识库检索结果 + 可用工具列表"后自行决策：如果检索结果中有相关文档就基于文档回答，如果没有就调用工具查数据库。比如用户问"退费流程"时向量库命中退费制度文档，模型基于文档回答；用户问"Java 班人数"时向量库无相关文档，模型调用 countStudentsByClazz 工具查 MySQL。

Q: 为什么用 Redis Stack 而不是 Milvus/Qdrant？

A: 项目已经用 Redis 做对话历史缓存和限流，Redis Stack 8.x 原生支持 HNSW 向量索引和 KNN 搜索。引入新中间件意味着部署复杂度、运维成本和学习成本上升，而 Redis Stack 的向量搜索性能对于教务系统这个数据量级（万级文档块）完全够用。如果数据量涨到百万级以上，可以平滑迁移到 Milvus，因为 Spring AI 的 VectorStore 是抽象接口，底层切换只需改配置。

Q: 你的对话记忆方案和直接全量注入有什么区别？

A: 全量注入有三个问题：token 浪费（40 条原文约 4000 token）、长对话溢出（超出上下文窗口）、无语义关联（按时间取的最近 40 条可能跟当前问题无关）。我的方案是三层记忆：近期 10 条原文保短期连贯性，向量库语义召回 5 条保跨会话关联，超过 20 轮时 LLM 压缩早期对话为摘要。合并后 token 消耗约 1500，比全量注入降低 60%+，而且语义召回能关联到"之前聊过张三"这种跨时间的上下文。

Q: Prompt Injection 怎么防？

A: 在 Advisor 链最外层挂了 SafeGuardAdvisor，在请求进入 ChatClient 之前做正则匹配。覆盖五类攻击模式：忽略指令、提示词窃取、角色覆盖、分隔符注入、中文攻击模式。检测到攻击时不是替换输入（那样会让模型困惑），而是直接抛异常返回安全提示。同时 Advisor 只记录攻击日志用于审计，不向用户暴露检测机制。

Q: 如果让你重新设计这个系统，你会改什么？

A: 最核心的是从一开始就用 Advisor 架构而不是手动构建 ChatClient。M6 版本时 Advisor 还不成熟，只能手动拼 system prompt + 注入历史 + 解析 <result> 标签，代码量大且每个能力都耦合在 Service 层。升级到 GA 版本后用 Advisor 链，RAG、记忆、安全、监控都是声明式挂载，新增能力只需加一个 Advisor 组件，不需要改核心调用链。其次是应该从 Day 1 就引入向量存储做知识库，而不是等到发现 Agent 无法回答制度类问题才补。

Q: 补齐的数据库表对 AI Agent 有什么价值？

A: 原来只有 7 张静态表，AI 只能回答"有多少人"这类统计问题。补齐考勤、成绩、违纪明细、缴费、就业等 9 张业务表后，AI 能力扩展到"管课管考管费管就业"——可以回答"今天谁旷课了""段誉考了多少分""学费交了吗""就业率多少"这类真实业务问题，这才是教务系统真正高频的需求。

八、风险与对策
风险	影响	对策
Spring AI GA API 变动大	M6→GA 有 breaking changes	先在分支上升级，逐步迁移，保留 M6 版本可回退
Redis Stack 未安装	向量搜索不可用	提供 SimpleVectorStore（内存）作为开发降级方案
Embedding API 不可用	知识库无法导入	支持多 embedding 提供商切换 + 本地模型降级
文档质量差影响 RAG 准确率	回答不准确	管理界面提供检索结果预览，管理员可验证和调整
RAG 与 Function Calling 冲突	模型走错路径	System Prompt 明确边界 + similarityThreshold 过滤 + 检索为空时引导
MCP 客户端兼容性	外部工具接入失败	MCP 是标准协议，优先测试 Claude Desktop 兼容性
Token 超额	成本失控	每用户每日配额 + 超额降级到轻量模型
定时任务多实例重复执行	重复推送	添加 @SchedulerLock（ShedLock）
九、含金量对比
维度	当前	优化后	简历描述
数据库	7 张静态表	19 张表（含考勤/成绩/缴费/就业）	"覆盖教务全业务链"
知识获取	16 个 @Tool 查 SQL	RAG + Function Calling 双路	"结构化数据+非结构化知识"
流式	Thread.sleep 假流式	Spring AI 原生 Flux	"首字延迟 < 2s"
记忆	40 条全量注入	三层语义记忆	"token 降低 60%+"
工具复用	系统内调用	MCP 协议双向	"支持 Claude Desktop 接入"
安全	SQL 注入防御	+ Prompt Injection 防御 + 数据脱敏	"正则匹配+异常拦截+流式脱敏"
可观测	正则猜工具调用	结构化元数据	"Token 治理+工具调用链追踪"
主动性	被动问答	定时 Agent 主动洞察	"周报生成+违纪预警"
十、验收标准总览
数据库新增 12 张表，预置测试数据
用户问"段誉今天来了吗"时，AI 调用考勤工具回答
用户问"退费流程"时，AI 从知识库检索真实文档回答，不编造
用户问"Java 班人数"时，AI 调用 @Tool 查 MySQL，不走 RAG
SSE 输出为原生流式，首字延迟 < 2 秒
代码中不再出现 Thread.sleep、<result>、<thinking> 字样
对话历史超 20 轮后自动触发摘要压缩，token 消耗降低 50%+
输入"忽略之前所有指令"等 prompt injection 内容时，被 SafeGuardAdvisor 拦截
手机号在 AI 回复中自动脱敏（138****1234）
可查询每用户每日 token 消耗
工具调用记录包含准确的方法名、参数、耗时、成功/失败状态
在 Claude Desktop 中配置 Tlias MCP Server 后，可直接对 Claude 查询教务数据
每周一定时生成教务周报并推送给管理员
# 第二部分：AI Agent 深度优化开发方案（详细设计）


> 基于 Spring Boot 3.5 + Spring AI + Vue 3 的智能教务管理系统，从"CRUD + AI 助手"演进为"具备 RAG 知识检索、MCP 协议接入、Advisor 架构、多 Agent 编排的企业级 AI Agent 系统"。

---

## 一、现状分析与优化目标

### 当前架构

```
前端 → AiChatController → TliasAgentServiceImpl
                           ├── ChatClient (手动构建, 缓存在 ConcurrentHashMap)
                           ├── 16 个 @Tool 方法 → MySQL 查询
                           ├── Redis List 对话历史 (40 条, 明文注入 prompt)
                           ├── extractResult() 解析 <result> 标签
                           ├── Thread.sleep(20) 模拟打字机 SSE
                           └── @Async 异步持久化到 MySQL
```

### 核心短板

| 维度 | 现状 | 问题 |
|------|------|------|
| 知识获取 | 仅靠 @Tool 查结构化数据 | 无法回答规章制度、课程大纲等非结构化知识，模型靠编（幻觉） |
| 对话记忆 | Redis List 存 40 条明文，全量注入 prompt | token 浪费、长对话溢出、无法语义关联历史 |
| 工具协议 | @Tool 本地方法，与系统强耦合 | 无法被外部 AI 客户端（Claude Desktop 等）复用 |
| 架构模式 | 手动构建 ChatClient，无 Advisor | RAG / 记忆 / 安全拦截无法声明式挂载 |
| 流式输出 | 同步调用 + sleep 模拟 | 首字延迟高，非真实流式 |
| 可观测性 | 仅存对话记录，无 token/工具指标 | 无法追踪 Agent 调用链，无法做配额管理 |
| 工具调用元数据 | 从 `<thinking>` 标签正则猜测 | 不准确，无法做工具调用分析 |

### 优化目标

将项目从"带 AI 聊天功能的教务系统"升级为"以 AI Agent 为核心的智能教务中台"，使 AI 真正嵌入业务闭环，而非停留在"问答助手"层面。

---

## 二、总体架构设计（优化后）

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端 (Vue 3 + SSE)                        │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                 POST /ai/chat (原生 SSE Flux)
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                     AiChatController                             │
│            (原生流式 + Flux<String> + SseEmitter)                 │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                  ChatClient (Advisor 链)                          │
│                                                                  │
│  ┌──────────┐  ┌──────────────┐  ┌────────────┐  ┌───────────┐ │
│  │ SafeGuard │→│ QuestionAns  │→│ ChatMemory  │→│  Tool     │ │
│  │ Advisor   │  │ Advisor(RAG)│  │ Advisor     │  │ Calling  │ │
│  │ (安全拦截) │  │ (向量检索)   │  │ (语义记忆)   │  │ (@Tool)  │ │
│  └──────────┘  └──────┬───────┘  └──────┬─────┘  └─────┬─────┘ │
│                       │                 │              │        │
│              ┌────────▼────────┐ ┌───────▼──────┐ ┌────▼──────┐ │
│              │  Vector Store   │ │ Vector Memory│ │ 16 Tools  │ │
│              │  (Redis Stack)  │ │ (Redis Stack) │ │ + MCP     │ │
│              │                 │ │               │ │  Client   │ │
│              │  教务知识库文档   │ │ 对话历史向量化  │ │           │ │
│              └────────┬────────┘ └──────────────┘ └─────┬─────┘ │
│                       │                           │             │
│              ┌────────▼────────┐         ┌─────────▼──────┐    │
│              │ Embedding Model  │         │  MCP Server     │    │
│              │ (DeepSeek Embed) │         │  (暴露 Tlias    │    │
│              └─────────────────┘         │   工具给外部)    │    │
│                                         └────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
         │                                    │
┌────────▼────────┐              ┌────────────▼────────────┐
│  Redis Stack     │              │     MySQL (业务数据)    │
│  (向量搜索+缓存)  │              │  dept/emp/clazz/student │
└─────────────────┘              └─────────────────────────┘
```

---

## 三、分阶段开发计划

### Phase 1：RAG 知识库 + 向量存储（核心突破）

**业务场景：** 教务系统有大量非结构化知识——退费流程说明、课程考核标准、学员管理制度、教师行为规范、常见问题 FAQ 等。用户问"退费流程是什么"，当前 Agent 无法回答（16 个 Tool 全是查结构化数据的），只能靠模型编造。RAG 让 Agent 能从真实文档中检索答案。

#### 3.1.1 引入向量存储

选择 Redis Stack 作为向量数据库，理由：项目已用 Redis，无需额外部署中间件；Redis Stack 8.x 原生支持 HNSW 向量索引和 KNN 搜索。

**pom.xml 新增依赖：**

```xml
<!-- Spring AI 向量存储 (Redis Stack) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-redis-store-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 文档读取 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-tika-document-reader</artifactId>
    <version>1.0.0</version>
</dependency>
```

**application.yml 新增：**

```yaml
spring:
  ai:
    vectorstore:
      redis:
        index: tlias-knowledge-base
        prefix: "tlias:doc:"
        initialize-schema: true
    embedding:
      openai:
        api-key: ${deepseek-api}
        base-url: https://api.deepseek.com/v1
        model: deepseek-embed  # 或使用其他兼容 embedding 端点
```

#### 3.1.2 知识库文档摄入管道

新建 `KnowledgeBaseService`，实现文档批量导入：

```java
@Service
public class KnowledgeBaseService {

    @Autowired
    private VectorStore vectorStore;

    /**
     * 批量导入教务知识文档
     * 支持 PDF / Word / TXT / Markdown
     */
    public void importDocuments(String filePath, String category) {
        // 1. 读取文档 (Tika 支持 PDF/Word/TXT 等)
        DocumentReader reader = new TikaDocumentReader(filePath);
        List<Document> documents = reader.get();

        // 2. 分块: 500 token/块, 100 token 重叠
        TokenTextSplitter splitter = new TokenTextSplitter(500, 100, 5, 10000, true);
        List<Document> chunks = splitter.apply(documents);

        // 3. 注入元数据 (分类、来源、时间戳)
        chunks.forEach(doc -> {
            doc.getMetadata().put("category", category);
            doc.getMetadata().put("source", filePath);
            doc.getMetadata().put("importTime", LocalDateTime.now().toString());
        });

        // 4. 向量化并存储 (Spring AI 自动调用 EmbeddingModel)
        vectorStore.add(chunks);
    }

    /**
     * 语义检索知识库
     */
    public List<Document> search(String query, String category, int topK) {
        SearchRequest request = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .similarityThreshold(0.7)
            .build();
        if (category != null) {
            request = request.withFilterExpression("category == '" + category + "'");
        }
        return vectorStore.similaritySearch(request);
    }
}
```

#### 3.1.3 接入 Agent 调用链

通过 `QuestionAnswerAdvisor` 自动注入检索结果，Agent 无需手动调用检索逻辑：

```java
@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel, VectorStore vectorStore,
                                  TliasAgentTools agentTools) {
        return ChatClient.builder(chatModel)
            .defaultTools(agentTools)
            .defaultAdvisors(
                // RAG: 每次提问自动检索知识库, 注入相关文档到上下文
                QuestionAnswerAdvisor.builder(vectorStore)
                    .searchRequest(SearchRequest.builder()
                        .topK(3)
                        .similarityThreshold(0.7)
                        .build())
                    .promptTemplate("""
                        根据以下教务知识库资料回答用户问题。
                        如果资料中没有相关信息, 请如实告知"知识库中未找到相关内容",
                        不要编造答案。
                        
                        【知识库资料】
                        {question_answer_context}
                        """)
                    .build()
            )
            .build();
    }
}
```

**效果：** 用户问"退费流程是什么"时，Advisor 自动从向量库检索到退费制度文档片段，注入到 prompt 上下文中，模型基于真实文档回答。同时 @Tool 依然可用——用户问"Java 班有多少人"时走 Tool 查 MySQL。两条路径自动切换，模型自行判断。

#### 3.1.4 知识库管理接口

新增 REST 接口供管理员上传文档：

```java
@RestController
@RequestMapping("/ai/knowledge")
public class KnowledgeController {

    @PostMapping("/import")
    public Result importDoc(@RequestParam("file") MultipartFile file,
                            @RequestParam("category") String category) {
        // 保存文件到临时路径
        // 调用 KnowledgeBaseService.importDocuments()
        // 返回导入结果 (分块数、向量化状态)
    }

    @GetMapping("/search")
    public Result search(@RequestParam String query,
                         @RequestParam(required = false) String category) {
        // 返回语义检索结果, 管理员可验证知识库内容
    }

    @DeleteMapping("/clear")
    public Result clear(@RequestParam String category) {
        // 按分类清理知识库向量
    }
}
```

**知识库分类设计：**

| 分类 | 内容 | 示例问题 |
|------|------|----------|
| `policy` | 学校规章制度、退费流程、请假制度 | "退费流程是什么" |
| `course` | 课程大纲、考核标准、教学计划 | "Java 课程的考核标准" |
| `faq` | 常见问题解答 | "怎么补办学生证" |
| `teacher` | 教师行为规范、考核制度 | "教师的考勤要求" |
| `student` | 学员管理制度、违纪处分条例 | "违纪扣分规则" |

---

### Phase 2：MCP 协议接入（生态扩展）

**业务场景：** 当前 Tlias 的工具方法只能在系统内部被 Agent 调用。教务老师如果想用 Claude Desktop 或 Cursor 查询教务数据，必须先登录 Tlias 网页。MCP 让 Tlias 的业务能力变成一个标准服务，任何 MCP 客户端都能接入。

#### 3.2.1 Tlias 作为 MCP Server（对外暴露工具）

将 16 个 @Tool 方法包装为 MCP Server 的 tools，通过 SSE 端点暴露：

```xml
<!-- pom.xml 新增 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-server-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

```yaml
# application.yml
spring:
  ai:
    mcp:
      server:
        name: tlias-education-server
        version: 1.0.0
        sse-endpoint: /mcp/sse
```

```java
@Component
public class TliasMcpTools {

    private final TliasAgentTools agentTools;

    @Tool(description = "根据学员ID查询学员详细信息")
    public String getStudentInfo(Integer studentId) {
        return agentTools.getStudentById(studentId);
    }

    @Tool(description = "按姓名模糊搜索学员")
    public String searchStudents(String name) {
        return agentTools.findStudentsByName(name);
    }

    @Tool(description = "统计各学历层次的学员人数分布")
    public String studentDegreeStats() {
        return agentTools.countStudentsByDegree();
    }

    @Tool(description = "查询指定班级下的所有学员")
    public String listStudentsByClass(Integer clazzId) {
        return agentTools.findStudentsByClazzId(clazzId);
    }

    // ... 包装其余 12 个工具方法
}
```

**使用方式：** 在 Claude Desktop 的配置文件中添加 Tlias MCP Server 地址，之后直接对 Claude 说"帮我查一下 Java 班有多少学员"，Claude 通过 MCP 协议调用 Tlias 的工具方法，返回真实数据。

#### 3.2.2 Tlias 作为 MCP Client（消费外部工具）

Agent 可以接入外部 MCP Server 扩展能力边界：

```java
@Configuration
public class McpClientConfig {

    @Bean
    public McpClient weatherMcpClient() {
        // 连接天气查询 MCP Server
        return McpClient.create("http://weather-mcp-server/sse");
    }
}

// 注册到 ChatClient
ChatClient.builder(chatModel)
    .defaultTools(agentTools)          // 本地工具
    .defaultTools(mcpClient.getTools()) // 外部 MCP 工具
    .build();
```

**实际业务场景：** 教务老师问"明天有极端天气吗，户外课程要不要调整"，Agent 通过外部天气 MCP Server 查到明天暴雨预警，结合 Tlias 的课程查询 Tool 返回"明天 Java-2024-01 班有户外团建课，建议改期"。

---

### Phase 3：Advisor 架构重构 + 原生流式

**业务场景：** 当前 `TliasAgentServiceImpl` 手动构建 ChatClient，代码臃肿，且无法声明式挂载 RAG、记忆、安全拦截等能力。重构为 Advisor 模式后，每个能力可插拔，代码量减少，架构更清晰。

#### 3.3.1 升级 Spring AI 版本

从 `1.0.0-M6` 升级到 `1.0.0 GA`：

```xml
<!-- pom.xml -->
<spring-ai.version>1.0.0</spring-ai.version>

<!-- 核心依赖 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>${spring-ai.version}</version>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-redis-store-spring-boot-starter</artifactId>
    <version>${spring-ai.version}</version>
</dependency>
```

#### 3.3.2 ChatClient 配置类（替代手动构建）

```java
@Configuration
public class ChatClientConfig {

    @Bean
    @Primary
    public ChatClient tliasChatClient(ChatModel chatModel,
                                       VectorStore vectorStore,
                                       ChatMemory chatMemory,
                                       TliasAgentTools agentTools) {
        return ChatClient.builder(chatModel)
            .defaultSystem(SYSTEM_PROMPT)
            .defaultTools(agentTools)
            .defaultAdvisors(
                // 1. 安全拦截: 防止 prompt injection
                new SafeGuardAdvisor(blockedPatterns),

                // 2. RAG 检索: 自动从知识库检索相关文档
                QuestionAnswerAdvisor.builder(vectorStore)
                    .searchRequest(SearchRequest.builder()
                        .topK(3)
                        .similarityThreshold(0.7)
                        .build())
                    .build(),

                // 3. 对话记忆: 自动管理上下文, 替代手动拼 prompt
                MessageChatMemoryAdvisor.builder(chatMemory)
                    .conversationId("dynamic")  // 运行时按 sessionId 设置
                    .build(),

                // 4. 自定义: Token 用量追踪 + 工具调用日志
                new TokenTrackingAdvisor(),
                new ToolCallLogAdvisor()
            )
            .build();
    }

    private static final String SYSTEM_PROMPT = """
        你是 Tlias 智能教务系统的 AI 助手。
        
        你的能力:
        1. 业务数据查询: 通过工具查询学员、员工、班级、部门数据
        2. 知识库问答: 基于检索到的教务制度文档回答问题
        3. 日常对话: 回答与业务无关的日常问题
        
        规则:
        - 查询业务数据时调用对应工具, 不要编造数据
        - 回答制度类问题时基于知识库资料, 资料中没有的如实告知
        - 涉及敏感操作(删除/修改)时, 必须向用户确认
        - 输出纯文本, 不暴露 SQL 或内部实现细节
        """;
}
```

#### 3.3.3 原生流式输出

废弃 `Thread.sleep(20)` 模拟打字机，改用 Spring AI 原生 `Flux<ChatResponse>`：

```java
@RestController
@RequestMapping("/ai")
public class AiChatController {

    @Autowired
    private ChatClient chatClient;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody AiChatRequest request) {

        return chatClient.prompt()
            .user(request.getMessage())
            .advisors(advisor -> advisor.param(
                ChatMemory.CONVERSATION_ID,
                request.getSessionId()
            ))
            .stream()
            .content()
            // 过滤空 chunk
            .filter(chunk -> chunk != null && !chunk.isEmpty());
    }
}
```

**效果：** 真正的首字延迟（TTFT）从"等模型生成完整回答"降到"模型输出第一个 token 即推送"，用户体感更快。同时去掉了 `<result>` 标签解析逻辑——原生流式不需要中间格式控制。

---

### Phase 4：语义对话记忆（替代 Redis List）

**业务场景：** 当前对话历史是 Redis List 存 40 条明文，全量注入 prompt。问题：用户上午问过"张三的违纪情况"，下午再问"他最近表现怎么样"，Agent 无法关联到上午的对话（因为 40 条可能已被新对话挤掉，或者即使还在也无语义关联）。

#### 3.4.1 向量化对话记忆

```java
@Service
public class SemanticChatMemory implements ChatMemory {

    @Autowired
    private VectorStore memoryVectorStore;

    @Autowired
    private StringRedisTemplate redis;

    private static final int RECALL_TOP_K = 5;

    @Override
    public void add(String conversationId, List<Message> messages) {
        // 每条消息向量化后存入向量库
        for (Message msg : messages) {
            Document doc = new Document(msg.getContent(), Map.of(
                "conversationId", conversationId,
                "role", msg.getMessageType().name(),
                "timestamp", Instant.now().toString()
            ));
            memoryVectorStore.add(List.of(doc));
        }

        // 同时存 Redis List 做近期上下文 (最近 10 条原文)
        // 向量库做长期语义召回
        // 两条路径互补
    }

    @Override
    public List<Message> get(String conversationId) {
        // 1. 从 Redis 取最近 10 条原文 (短期记忆)
        List<Message> recent = getRecentFromRedis(conversationId, 10);

        // 2. 从向量库语义检索最相关的 5 条 (长期记忆)
        //    用当前 session 最后一条用户消息做查询
        String lastUserMsg = recent.stream()
            .filter(m -> m.getMessageType() == MessageType.USER)
            .reduce((first, second) -> second)
            .map(Message::getContent)
            .orElse("");

        List<Document> recalled = memoryVectorStore.similaritySearch(
            SearchRequest.builder()
                .query(lastUserMsg)
                .topK(RECALL_TOP_K)
                .similarityThreshold(0.65)
                .build()
        );

        // 3. 合并去重: 近期原文 + 语义召回的长期记忆
        return mergeMemories(recent, recalled);
    }

    @Override
    public void clear(String conversationId) {
        // 清理 Redis + 向量库中该会话的所有记录
    }
}
```

#### 3.4.2 对话摘要压缩

当历史超过 20 轮时，触发摘要压缩：

```java
@Service
public class MemorySummarizer {

    @Autowired
    private ChatModel chatModel;

    /**
     * 将早期对话压缩为摘要, 释放上下文窗口
     */
    public String summarize(List<Message> oldMessages) {
        String conversationText = oldMessages.stream()
            .map(m -> m.getMessageType() + ": " + m.getContent())
            .collect(Collectors.joining("\n"));

        return chatModel.call("""
            请将以下对话历史压缩为简洁摘要, 保留关键信息:
            
            %s
            
            摘要要求:
            - 保留用户关注的核心主题
            - 保留涉及的人名、班级名等关键实体
            - 保留用户的偏好和意图
            - 控制在 200 字以内
            """.formatted(conversationText));
    }
}
```

**记忆架构：**

```
对话记忆 = 近期原文(Redis List, 最近10条) 
         + 语义召回(向量库, 相关5条)
         + 历史摘要(LLM压缩, 1段200字)
```

三者合并后注入上下文，token 消耗从"40条原文约4000 token"降到"10条原文+5条召回+1段摘要约1500 token"。

---

### Phase 5：可观测性与安全加固

#### 3.5.1 Token 用量追踪

```java
@Component
public class TokenTrackingAdvisor implements BaseAdvisor {

    @Autowired
    private StringRedisTemplate redis;

    private static final String TOKEN_KEY = "ai:tokens:daily:";

    @Override
    public AdvisedResponse after(AdvisedResponse advisedResponse) {
        Usage usage = advisedResponse.response().getMetadata().getUsage();

        String today = LocalDate.now().toString();
        String userId = getCurrentUserId();

        // 按用户+模型+日期统计 token 消耗
        redis.opsForHash().increment(
            TOKEN_KEY + today + ":" + userId,
            "promptTokens",
            usage.getPromptTokens()
        );
        redis.opsForHash().increment(
            TOKEN_KEY + today + ":" + userId,
            "completionTokens",
            usage.getCompletionTokens()
        );

        // 超额告警
        Long totalToday = getTotalTokensToday(userId);
        if (totalToday > 50000) {
            log.warn("用户 {} 今日 token 用量已达 {}", userId, totalToday);
        }

        return advisedResponse;
    }
}
```

#### 3.5.2 结构化工具调用日志

废弃从 `<thinking>` 标签正则猜测工具调用，改用 Spring AI 结构化元数据：

```java
@Component
public class ToolCallLogAdvisor implements BaseAdvisor {

    @Autowired
    private ChatRecordMapper chatRecordMapper;

    @Override
    public AdvisedResponse after(AdvisedResponse advisedResponse) {
        // 从结构化响应中提取工具调用信息
        List<ToolCall> toolCalls = advisedResponse.response()
            .getMetadata()
            .getToolCalls();  // 准确的工具调用记录

        if (!toolCalls.isEmpty()) {
            ToolCallRecord record = new ToolCallRecord();
            record.setUserId(getCurrentUserId());
            record.setSessionId(getCurrentSessionId());
            record.setToolNames(toolCalls.stream()
                .map(ToolCall::name)
                .collect(Collectors.joining(",")));
            record.setToolArgs(toolCalls.stream()
                .map(tc -> tc.name() + ":" + tc.arguments())
                .collect(Collectors.joining(" | ")));
            record.setCallTime(LocalDateTime.now());
            record.setSuccess(true);

            toolCallMapper.insert(record);
        }

        return advisedResponse;
    }
}
```

#### 3.5.3 Prompt Injection 防御

```java
@Component
public class SafeGuardAdvisor implements BaseAdvisor {

    private static final List<Pattern> BLOCKED_PATTERNS = List.of(
        // 忽略之前所有指令
        Pattern.compile("(?i)ignore\\s+(?:all\\s+)?(?:previous|prior)\\s+instructions"),
        // 系统提示词窃取
        Pattern.compile("(?i)(?:show|reveal|print|repeat)\\s+(?:your\\s+)?(?:system\\s+)?prompt"),
        // 角色覆盖
        Pattern.compile("(?i)you\\s+are\\s+now\\s+(?:DAN|developer\\s+mode|jailbreak)"),
        // 分隔符注入
        Pattern.compile("---+\\s*system\\s*---+")
    );

    @Override
    public AdvisedRequest before(AdvisedRequest advisedRequest) {
        String userInput = advisedRequest.requestParams().getUserText();

        for (Pattern pattern : BLOCKED_PATTERNS) {
            if (pattern.matcher(userInput).find()) {
                log.warn("检测到 prompt injection 尝试: userId={}, input={}",
                    getCurrentUserId(),
                    userInput.substring(0, Math.min(100, userInput.length())));

                // 替换为安全提示
                advisedRequest.requestParams().setUserText(
                    "用户输入包含不安全内容, 请礼貌地告知用户你只能回答教务相关问题。"
                );
                break;
            }
        }

        return advisedRequest;
    }
}
```

#### 3.5.4 敏感数据脱敏

```java
@Component
public class DataMaskingAdvisor implements BaseAdvisor {

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("(?<=\\d{3})\\d{4}(?=\\d{4})");
    private static final Pattern ID_CARD_PATTERN =
        Pattern.compile("(?<=\\d{6})\\d{8}(?=\\d{4})");

    @Override
    public AdvisedResponse after(AdvisedResponse advisedResponse) {
        String content = advisedResponse.response().getResult().getOutput().getText();

        // 手机号脱敏: 138****1234
        content = PHONE_PATTERN.matcher(content).replaceAll("****");
        // 身份证号脱敏: 110101********1234
        content = ID_CARD_PATTERN.matcher(content).replaceAll("********");

        return advisedResponse;
    }
}
```

---

### Phase 6：定时 Agent 任务与主动洞察

**业务场景：** 当前 Agent 是被动的——用户不问就不动。但教务场景有很多需要主动推送的洞察：每周违纪统计报告、学员异动预警、班级出勤异常提醒。

#### 3.6.1 定时洞察任务

```java
@Service
public class ScheduledAgentService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private TliasAgentTools agentTools;

    /**
     * 每周一早上 8 点生成周报
     */
    @Scheduled(cron = "0 0 8 ? * MON")
    public void weeklyReport() {
        // Agent 主动调用工具收集数据
        String degreeStats = agentTools.countStudentsByDegree();
        String clazzStats = agentTools.countStudentsByClazz();
        String empGenderStats = agentTools.countEmpsByGender();

        String prompt = """
            请基于以下数据生成本周教务周报:
            
            学历分布: %s
            班级分布: %s
            员工性别分布: %s
            
            要求:
            1. 概述本周教务整体情况
            2. 标注异常数据(如某班级人数过少/违纪过多)
            3. 给出管理建议
            """.formatted(degreeStats, clazzStats, empGenderStats);

        String report = chatClient.prompt().user(prompt).call().content();

        // 推送给管理员(站内消息 / 邮件 / 钉钉)
        notificationService.sendToAdmins(report);
    }

    /**
     * 每天检查违纪异常增长
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void violationAlert() {
        // Agent 自主分析: 查询今日违纪数据, 与历史均值对比
        // 超过阈值则生成预警报告并推送
    }
}
```

#### 3.6.2 Agent 自主决策工作流

对于复杂任务，引入 Plan-and-Execute 模式：

```java
@Service
public class PlanAndExecuteAgent {

    /**
     * 复杂任务: "分析 Java 方向所有班级的学员情况并给出优化建议"
     * Agent 自动拆解为多个步骤执行
     */
    public String executeComplexTask(String task) {
        // Step 1: 规划
        String plan = chatClient.prompt()
            .user("请将以下任务拆解为具体步骤: " + task)
            .call().content();

        // Step 2: 逐步执行
        // Step 3: 汇总结果
        // Step 4: 生成最终报告
    }
}
```

---

## 四、新增数据库表设计

### 4.1 知识库管理表

```sql
CREATE TABLE tlias_knowledge_doc (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name   VARCHAR(255) NOT NULL COMMENT '原始文件名',
    category    VARCHAR(50)  NOT NULL COMMENT '分类: policy/course/faq/teacher/student',
    chunk_count INT          NOT NULL COMMENT '分块数量',
    status      VARCHAR(20)  DEFAULT 'PROCESSING' COMMENT 'PROCESSING/READY/FAILED',
    file_path   VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '知识库文档管理';
```

### 4.2 工具调用记录表

```sql
CREATE TABLE tlias_tool_call_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     INT     NOT NULL COMMENT '调用者',
    session_id  VARCHAR(100) NOT NULL COMMENT '会话ID',
    tool_name   VARCHAR(100) NOT NULL COMMENT '工具方法名',
    tool_args   TEXT        NOT NULL COMMENT '调用参数(JSON)',
    result      TEXT        NULL COMMENT '返回结果(截断)',
    duration_ms BIGINT      NULL COMMENT '执行耗时(毫秒)',
    success     TINYINT(1)  DEFAULT 1 COMMENT '1成功 0失败',
    error_msg   VARCHAR(500) NULL COMMENT '失败原因',
    call_time   DATETIME    DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, call_time),
    INDEX idx_tool (tool_name)
) COMMENT 'AI工具调用日志';
```

### 4.3 Token 用量统计表

```sql
CREATE TABLE tlias_token_usage (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id          INT     NOT NULL,
    model_name       VARCHAR(50) NOT NULL COMMENT 'deepseek/mimo/longcat',
    prompt_tokens    INT     NOT NULL COMMENT '输入token',
    completion_tokens INT    NOT NULL COMMENT '输出token',
    total_tokens     INT     NOT NULL COMMENT '总token',
    session_id       VARCHAR(100) NULL,
    record_time      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, record_time),
    INDEX idx_model (model_name)
) COMMENT 'AI Token用量统计';
```

---

## 五、简历项目描述（优化后）

### 项目名称

Tlias 智能教务管理系统 — AI Agent 方向

### 项目描述

基于 Spring Boot 3.5 + Spring AI 1.0 GA + Vue 3 的智能教务管理平台，以 AI Agent 为核心驱动业务查询、知识问答和主动洞察。系统采用 RAG + Function Calling 双路知识获取架构，通过 MCP 协议实现工具跨平台复用，具备语义对话记忆、Prompt Injection 防御、Token 用量治理等生产级能力。

### 核心技术亮点

**1. RAG + Function Calling 双路知识获取**
- 结构化业务数据（学员/员工/班级/部门）走 Function Calling，Agent 自动调用 16 个 @Tool 方法查 MySQL
- 非结构化知识（规章制度/课程大纲/FAQ）走 RAG 语义检索，文档经 Tika 解析 + TokenTextSplitter 分块后向量化存入 Redis Stack，通过 QuestionAnswerAdvisor 自动注入检索结果
- 两条路径由模型自动判断切换，覆盖"查数据"和"查制度"两类场景

**2. MCP 协议双向接入**
- 作为 MCP Server 将 16 个教务工具暴露为标准协议，支持 Claude Desktop / Cursor 等外部 AI 客户端直接查询教务数据
- 作为 MCP Client 接入外部工具服务（天气/日历），扩展 Agent 能力边界
- 实现工具定义与模型解耦，同一套业务工具支持多 AI 客户端复用

**3. Advisor 架构 + 原生流式**
- 采用 Spring AI Advisor 链式架构，声明式挂载 SafeGuardAdvisor（Prompt Injection 防御）、QuestionAnswerAdvisor（RAG 检索）、ChatMemoryAdvisor（对话记忆）、TokenTrackingAdvisor（用量追踪）
- 从同步调用 + Thread.sleep 模拟打字机重构为原生 Flux 流式输出，首字延迟从"等完整回答"降到"首个 token 即推送"
- 通过 Advisor 模式实现能力可插拔，新增能力只需添加一个 Advisor 组件

**4. 语义对话记忆**
- 短期记忆：Redis List 存最近 10 轮原文
- 长期记忆：向量库存储全部历史，按当前问题语义检索召回 5 条最相关对话
- 历史摘要：超过 20 轮时用 LLM 压缩早期对话为 200 字摘要
- 三层合并后注入上下文，token 消耗降低 60%+，同时支持跨会话语义关联

**5. 安全与可观测性**
- Prompt Injection 防御：正则匹配 + 输入替换，拦截"忽略之前指令""角色覆盖"等攻击
- 敏感数据脱敏：Advisor 层对手机号/身份证号自动脱敏
- Token 用量治理：按用户+模型+日期维度统计，超额告警
- 结构化工具调用日志：从 `<thinking>` 标签正则猜测升级为 Spring AI 结构化 ToolCall 元数据

**6. 定时 Agent 主动洞察**
- 每周自动生成教务周报：Agent 主动调用工具收集数据 + LLM 分析 + 异常标注 + 管理建议
- 每日违纪异常预警：Agent 对比历史均值，超阈值主动推送

### 技术栈

| 层面 | 技术 |
|------|------|
| 后端 | Spring Boot 3.5, Spring AI 1.0 GA, MyBatis, MySQL 8.0, Redis Stack |
| AI | ChatClient + Advisor 链, Function Calling, RAG (VectorStore + Embedding), MCP Server/Client, QuestionAnswerAdvisor |
| 向量存储 | Redis Stack (HNSW 索引, KNN 搜索) |
| 文档处理 | Apache Tika (PDF/Word/TXT 解析), TokenTextSplitter (分块) |
| 安全 | JWT, Prompt Injection 防御, 数据脱敏, RBAC |
| 监控 | Token 追踪, 工具调用日志, Micrometer |
| 前端 | Vue 3, Vite, Element Plus, EventSource (SSE) |

---

## 六、开发排期与里程碑

| 阶段 | 内容 | 预估工时 | 产出 |
|------|------|----------|------|
| Phase 1 | RAG 知识库 + 向量存储 | 3-4 天 | KnowledgeBaseService + QuestionAnswerAdvisor + 知识库管理接口 + 5 份测试文档 |
| Phase 2 | MCP Server + Client | 2-3 天 | TliasMcpTools + MCP Server 配置 + Claude Desktop 联调 |
| Phase 3 | Advisor 重构 + 原生流式 | 3-4 天 | ChatClientConfig + Advisor 链 + Controller 原生 SSE + 废弃手动构建 |
| Phase 4 | 语义对话记忆 | 2-3 天 | SemanticChatMemory + MemorySummarizer + 记忆三层合并 |
| Phase 5 | 安全与可观测性 | 2-3 天 | SafeGuardAdvisor + DataMaskingAdvisor + TokenTrackingAdvisor + 工具调用日志表 |
| Phase 6 | 定时 Agent 任务 | 1-2 天 | ScheduledAgentService + 周报生成 + 违纪预警 |
| **合计** | | **13-19 天** | |

### 验收标准

- [ ] 用户可上传教务文档（PDF/Word），系统自动分块向量化存入知识库
- [ ] 用户问"退费流程"等问题时，Agent 从知识库检索真实文档回答，不编造
- [ ] 用户问"Java 班有多少人"时，Agent 调用 @Tool 查 MySQL，不走 RAG
- [ ] 在 Claude Desktop 中配置 Tlias MCP Server 后，可直接对 Claude 查询教务数据
- [ ] SSE 输出为原生流式，首字延迟 < 2 秒
- [ ] 对话历史超 20 轮后自动触发摘要压缩，token 消耗降低 50%+
- [ ] 输入"忽略之前所有指令"等 prompt injection 内容时，被 SafeGuardAdvisor 拦截
- [ ] 手机号在 AI 回复中自动脱敏（138****1234）
- [ ] 每周一定时生成教务周报并推送给管理员
- [ ] 工具调用记录包含准确的方法名、参数、耗时、成功/失败状态

---

## 七、面试核心问答准备

**Q: 你的 Agent 怎么决定走 RAG 还是 Function Calling？**

A: 我不需要手动判断。Spring AI 的 QuestionAnswerAdvisor 会在每次请求时自动执行向量检索，检索结果作为上下文注入。同时 @Tool 方法注册在 ChatClient 上。模型拿到"用户问题 + 知识库检索结果 + 可用工具列表"后自行决策：如果检索结果中有相关文档就基于文档回答，如果没有就调用工具查数据库。比如用户问"退费流程"时向量库命中退费制度文档，模型基于文档回答；用户问"Java 班人数"时向量库无相关文档，模型调用 `countStudentsByClazz` 工具查 MySQL。

**Q: 为什么用 Redis Stack 而不是 Milvus/Qdrant？**

A: 项目已经用 Redis 做对话历史缓存和限流，Redis Stack 8.x 原生支持 HNSW 向量索引和 KNN 搜索。引入新中间件意味着部署复杂度、运维成本和学习成本上升，而 Redis Stack 的向量搜索性能对于教务系统这个数据量级（万级文档块）完全够用。如果数据量涨到百万级以上，可以平滑迁移到 Milvus，因为 Spring AI 的 VectorStore 是抽象接口，底层切换只需改配置。

**Q: MCP 在你的项目里解决了什么问题？**

A: 核心解决"工具复用"问题。之前 16 个 @Tool 方法只能在 Tlias 系统内部被 Agent 调用，教务老师想用 AI 查数据必须登录 Tlias 网页。做完 MCP Server 后，老师在 Claude Desktop、Cursor 等任意支持 MCP 的 AI 客户端里直接对话就能查询教务数据，Tlias 从"一个带 AI 的 Web 应用"变成了"一个可以被任何 AI 客户端接入的教务数据服务"。同时作为 MCP Client 接入外部工具（天气、日历），Agent 能力边界从教务数据扩展到跨域协同。

**Q: 你的对话记忆方案和直接全量注入有什么区别？**

A: 全量注入有三个问题：token 浪费（40 条原文约 4000 token）、长对话溢出（超出上下文窗口）、无语义关联（按时间取的最近 40 条可能跟当前问题无关）。我的方案是三层记忆：近期 10 条原文保短期连贯性，向量库语义召回 5 条保跨会话关联，超过 20 轮时 LLM 压缩早期对话为摘要。合并后 token 消耗约 1500，比全量注入降低 60%+，而且语义召回能关联到"之前聊过张三"这种跨时间的上下文。

**Q: Prompt Injection 怎么防？**

A: 在 Advisor 链最外层挂了 SafeGuardAdvisor，在请求进入 ChatClient 之前做正则匹配。覆盖五类攻击模式：忽略指令（"ignore previous instructions"）、提示词窃取（"show your prompt"）、角色覆盖（"you are now DAN"）、分隔符注入（"---system---"）、指令嵌套。检测到攻击时不是拒绝回答（那样用户体验差），而是把用户输入替换为安全提示，让模型礼貌地引导用户回到教务话题。同时 Advisor 只记录攻击日志用于审计，不向用户暴露检测机制。

**Q: 如果让你重新设计这个系统，你会改什么？**

A: 最核心的是从一开始就用 Advisor 架构而不是手动构建 ChatClient。M6 版本时 Advisor 还不成熟，只能手动拼 system prompt + 注入历史 + 解析 `<result>` 标签，代码量大且每个能力都耦合在 Service 层。升级到 GA 版本后用 Advisor 链，RAG、记忆、安全、监控都是声明式挂载，新增能力只需加一个 Advisor 组件，不需要改核心调用链。其次是应该从 Day 1 就引入向量存储做知识库，而不是等到发现 Agent 无法回答制度类问题才补。

---

## 八、风险与对策

| 风险 | 影响 | 对策 |
|------|------|------|
| Spring AI GA API 变动大 | M6→GA 有 breaking changes | 先在分支上升级，逐步迁移，保留 M6 版本可回退 |
| Redis Stack 未安装 | 向量搜索不可用 | 提供 SimpleVectorStore（内存）作为开发降级方案 |
| Embedding API 不可用 | 知识库无法导入 | 支持多 embedding 提供商切换 + 本地模型降级 |
| 文档质量差影响 RAG 准确率 | 回答不准确 | 管理界面提供检索结果预览，管理员可验证和调整 |
| MCP 客户端兼容性 | 外部工具接入失败 | MCP 是标准协议，优先测试 Claude Desktop 兼容性 |
| Token 超额 | 成本失控 | 每用户每日配额 + 超额降级到轻量模型 |
