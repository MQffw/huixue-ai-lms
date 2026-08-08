# Tlias AI Agent 深度优化开发方案

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
