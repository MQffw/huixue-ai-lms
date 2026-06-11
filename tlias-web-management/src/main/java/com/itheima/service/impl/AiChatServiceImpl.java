package com.itheima.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itheima.security.sql.SecureSqlExecutor;
import com.itheima.security.sql.SqlSecurityException;
import com.itheima.security.sql.SqlSecurityValidator;
import com.itheima.service.AiChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.api-url}")
    private String apiUrl;

    @Value("${ai.model}")
    private String model;

    @Value("${ai.max-tokens}")
    private int maxTokens;

    @Value("${ai.temperature}")
    private double temperature;

    @Autowired
    private SecureSqlExecutor secureSqlExecutor;

    @Autowired
    private SqlSecurityValidator sqlSecurityValidator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            你是 Tlias 智能学习辅助系统的 AI 助手。

            重要规则：
            1. 禁止使用任何 Markdown 格式，用纯文本回答
            2. 禁止调用任何工具或函数
            3. 回答简洁明了，用中文
            4. 当用户询问业务数据时，你只能回复一条可执行的 SQL 语句，不要加任何解释文字，不要编造数据
            5. SQL 语句必须严格遵循安全规范：
               - 只允许 SELECT 查询，禁止 DROP/ALTER/DELETE/TRUNCATE/UPDATE/INSERT/REPLACE/MERGE 等危险操作
               - 只能访问以下表：emp、dept、student、clazz，禁止访问其他表或系统表
               - 禁止使用 UNION、INTO OUTFILE、LOAD_FILE、BENCHMARK、SLEEP、WAITFOR、SHUTDOWN 等危险关键字
               - 禁止使用的函数：CHAR()、CONCAT()、HEX()、UNHEX()、ASCII()、ORD()、LOAD_FILE()、INTO OUTFILE
               - 不要包含任何注释（如 -- 或 /* */）
               - 不要使用子查询嵌套（最多允许2层）
               - 使用参数化查询格式（如 WHERE id = ?）
               - SQL 语句必须简单直接，避免复杂函数调用

            数据库表结构：
            - dept: id, name, create_time, update_time （部门表）
            - emp: id, username, password, name, gender, phone, job, salary, image, entry_date, dept_id, create_time, update_time （员工表，gender：1男 2女）
            - clazz: id, name, room, begin_date, end_date, master_id, subject, create_time, update_time （班级表）
            - student: id, name, no, gender, phone, id_card, is_college, address, degree, graduation_date, clazz_id, create_time, update_time （学生表，gender：1男 2女）

            示例：
            用户：有多少个部门？
            回复：SELECT COUNT(*) FROM dept

            用户：学生男女比例
            回复：SELECT gender, COUNT(*) as count FROM student GROUP BY gender

            用户：查找名字包含"张"的学生
            回复：SELECT * FROM student WHERE name LIKE '%张%'
            """;

    @Override
    public SseEmitter streamChat(String message, List<Map<String, String>> history) {
        SseEmitter emitter = new SseEmitter(120_000L);
        AtomicBoolean completed = new AtomicBoolean(false);

        emitter.onCompletion(() -> completed.set(true));
        emitter.onTimeout(() -> completed.set(true));
        emitter.onError(e -> completed.set(true));

        CompletableFuture.runAsync(() -> {
            try {
                // 先用同步方式获取 AI 响应
                String aiResponse = chatSync(message, history, SYSTEM_PROMPT);

                if (completed.get()) return;

                // 检查是否包含 SQL
                if (aiResponse.contains("SELECT") || aiResponse.contains("select")) {
                    String sql = extractSql(aiResponse);
                    if (sql != null && !sql.isEmpty()) {
                        try {
                            String queryResult = executeQuery(sql);
                            if (queryResult != null && !queryResult.startsWith("[安全限制]") && !queryResult.startsWith("[查询失败]")) {
                                // 将查询结果作为上下文再次发给 AI
                                String followUpPrompt = "查询结果如下：\n" + queryResult + "\n请用自然语言回答用户的问题。不要显示 SQL 语句。";
                                String finalAnswer = chatSync(followUpPrompt, history, "你是 Tlias 智能学习辅助系统的 AI 助手，请用简洁的中文回答用户问题。不要显示 SQL 语句，直接回答结果。");
                                emitter.send(finalAnswer);
                            } else {
                                emitter.send(aiResponse);
                            }
                        } catch (Exception e) {
                            log.warn("执行 SQL 查询失败: {}", sql, e);
                            emitter.send("查询失败: " + e.getMessage());
                        }
                    } else {
                        emitter.send(aiResponse);
                    }
                } else {
                    // 普通聊天，逐字发送模拟流式效果
                    for (char c : aiResponse.toCharArray()) {
                        if (completed.get()) break;
                        emitter.send(String.valueOf(c));
                        Thread.sleep(30);
                    }
                }

                if (!completed.get()) {
                    emitter.complete();
                }
            } catch (Exception e) {
                log.error("AI 聊天异常", e);
                try {
                    emitter.send("[错误] " + e.getMessage());
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        return emitter;
    }

    @Override
    public String chat(String message, List<Map<String, String>> history) {
        return chatSync(message, history, SYSTEM_PROMPT);
    }

    private String chatSync(String message, List<Map<String, String>> history, String systemPrompt) {
        try {
            ObjectNode requestBody = buildRequestBody(message, history, systemPrompt, false);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .timeout(Duration.ofSeconds(120))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode jsonNode = objectMapper.readTree(response.body());
            String aiResponse = jsonNode.at("/choices/0/message/content").asText();

            // 过滤工具调用标签
            String filteredResponse = filterContent(aiResponse);

            // 检查是否包含 SQL，执行数据问答
            if (filteredResponse.contains("SELECT") || filteredResponse.contains("select")) {
                String sql = extractSql(filteredResponse);
                log.info("提取的 SQL: {}", sql);
                if (sql != null && !sql.isEmpty()) {
                    try {
                        String queryResult = executeQuery(sql);
                        log.info("查询结果: {}", queryResult);
                        if (queryResult != null && !queryResult.startsWith("[安全限制]") && !queryResult.startsWith("[查询失败]")) {
                            // 将查询结果作为上下文再次发给 AI
                            String followUpPrompt = "查询结果如下：\n" + queryResult + "\n请用自然语言回答用户的问题。";
                            return chatSync(followUpPrompt, history, "你是 Tlias 智能学习辅助系统的 AI 助手，请用简洁的中文回答用户问题。");
                        }
                    } catch (Exception e) {
                        log.warn("执行 SQL 查询失败: {}", sql, e);
                        return filteredResponse + "\n\n[查询失败: " + e.getMessage() + "]";
                    }
                }
            }

            return filteredResponse;
        } catch (Exception e) {
            log.error("AI 同步聊天异常", e);
            return "[错误] " + e.getMessage();
        }
    }

    private String filterContent(String content) {
        if (content == null || content.isEmpty()) return "";

        // 过滤 longcat 工具调用标签
        content = content.replaceAll("<longcat_tool_call>.*?</longcat_tool_call>", "");
        content = content.replaceAll("<longcat_tool_call>.*?<", "<");
        content = content.replaceAll("<longcat_tool_call>.*", "");
        content = content.replaceAll("<longcat_arg_key>.*?</longcat_arg_key>", "");

        // 过滤 Markdown 格式符号
        content = content.replace("**", "");
        content = content.replace("##", "");
        content = content.replace("```sql", "");
        content = content.replace("```", "");

        return content.trim();
    }

    private ObjectNode buildRequestBody(String message, List<Map<String, String>> history, String systemPrompt, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);
        body.put("stream", stream);

        ArrayNode messages = objectMapper.createArrayNode();

        // 系统提示
        ObjectNode systemMsg = objectMapper.createObjectNode();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        // 历史消息
        if (history != null) {
            for (Map<String, String> msg : history) {
                ObjectNode historyMsg = objectMapper.createObjectNode();
                historyMsg.put("role", msg.get("role"));
                historyMsg.put("content", msg.get("content"));
                messages.add(historyMsg);
            }
        }

        // 当前消息
        ObjectNode userMsg = objectMapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", message);
        messages.add(userMsg);

        body.set("messages", messages);
        return body;
    }

    private String extractSql(String text) {
        // 从 AI 响应中提取 SQL 语句
        int start = text.indexOf("```sql");
        if (start == -1) {
            start = text.indexOf("```");
            if (start == -1) {
                // 尝试直接提取 SELECT 语句
                start = text.toUpperCase().indexOf("SELECT");
                if (start == -1) return null;
                int end = text.indexOf(";", start);
                if (end == -1) end = text.length();
                String sql = text.substring(start, end).trim();
                return fixSql(sql);
            }
            start += 3;
        } else {
            start += 6;
        }

        int end = text.indexOf("```", start);
        if (end == -1) end = text.length();

        String sql = text.substring(start, end).trim();
        // 移除末尾的分号
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        return fixSql(sql);
    }

    private String fixSql(String sql) {
        if (sql == null || sql.isEmpty()) return sql;

        // 修复 AI 生成的 SQL 中常见的缺少空格问题
        // SELECT 后面必须有空格
        sql = sql.replaceAll("(?i)(SELECT)([A-Z(])", "$1 $2");
        // FROM 前后必须有空格
        sql = sql.replaceAll("(?i)([A-Z*)])(FROM)", "$1 $2");
        sql = sql.replaceAll("(?i)(FROM)([A-Z])", "$1 $2");
        // WHERE 前后必须有空格
        sql = sql.replaceAll("(?i)([A-Z0-9)])(WHERE)", "$1 $2");
        sql = sql.replaceAll("(?i)(WHERE)([A-Z(])", "$1 $2");
        // GROUP BY 必须有空格
        sql = sql.replaceAll("(?i)(GROUP)(BY)", "$1 $2");
        // ORDER BY 必须有空格
        sql = sql.replaceAll("(?i)(ORDER)(BY)", "$1 $2");
        // HAVING 前后必须有空格
        sql = sql.replaceAll("(?i)([A-Z0-9)])(HAVING)", "$1 $2");
        // AS 前后必须有空格
        sql = sql.replaceAll("(?i)([A-Z0-9)])(AS)([A-Z(])", "$1 $2 $3");
        // AND/OR 前后必须有空格
        sql = sql.replaceAll("(?i)([A-Z0-9)])(AND|OR)([A-Z(])", "$1 $2 $3");
        // 逗号后面加空格
        sql = sql.replaceAll(",([A-Z])", ", $1");

        return sql.trim();
    }

    /**
     * 使用安全执行器执行 SQL 查询
     * @param sql AI 生成的 SQL 语句
     * @return 查询结果或错误信息
     */
    private String executeQuery(String sql) {
        try {
            // 使用安全执行器（包含白名单校验、预编译、注入防御）
            return secureSqlExecutor.executeQuery(sql);
        } catch (Exception e) {
            log.error("安全SQL执行器异常", e);
            return "[查询失败] " + e.getMessage();
        }
    }
}
