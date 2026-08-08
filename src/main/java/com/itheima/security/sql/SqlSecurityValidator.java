package com.itheima.security.sql;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SQL 安全校验器（旧模式遗留 - NLP转SQL方案）
 * 当前系统已切换为 Spring AI Agent + Tool 模式，此类不再被主流程调用
 * 保留作为备用，如需恢复 SQL 生成模式可重新启用
 */
@Slf4j
@Component
public class SqlSecurityValidator {
    
    private static final Set<String> ALLOWED_TABLES = new HashSet<>(Arrays.asList(
            "emp", "dept", "student", "clazz", "emp_expr",
            "course", "course_schedule", "attendance", "exam", "score",
            "violation_log", "payment", "employment", "notice",
            "tlias_knowledge_doc", "tlias_tool_call_log", "tlias_token_usage"
    ));
    
    private static final Set<String> DANGEROUS_KEYWORDS = new HashSet<>(Arrays.asList(
            "UNION", "INTO", "OUTFILE", "LOAD_FILE", "BENCHMARK",
            "SLEEP", "DELAY", "WAITFOR", "SHUTDOWN", "GRANT",
            "REVOKE", "CREATE", "ALTER", "DROP", "TRUNCATE",
            "DELETE", "UPDATE", "INSERT", "REPLACE", "MERGE",
            "EXEC", "EXECUTE", "CALL", "DECLARE", "CURSOR",
            "INFORMATION_SCHEMA", "MYSQL", "SYS", "PG_",
            "--", "/*", "*/", ";", "@@", "CHAR(", "CONCAT(",
            "0x", "HEX(", "UNHEX(", "ASCII(", "ORD(",
            "DATABASE()", "SCHEMA()", "USER()", "VERSION()",
            "NCHAR(", "NVARCHAR(", "CAST(", "CONVERT(",
            "LOAD DATA", "LOCK TABLE", "UNLOCK TABLE",
            "HAVING", "PROCEDURE", "FUNCTION", "TRIGGER"
    ));
    
    public void validate(String sql) throws SqlSecurityException {
        if (sql == null || sql.trim().isEmpty()) {
            throw new SqlSecurityException("SQL语句不能为空");
        }

        // 多语句拆分：分号后带 DDL/DML 的情况需拦截
        String[] statements = sql.split(";");
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (trimmed.isEmpty()) continue;
            validateSingle(trimmed);
        }
    }

    private void validateSingle(String sql) throws SqlSecurityException {
        String upperSql = sql.toUpperCase().trim();

        if (!upperSql.startsWith("SELECT")) {
            throw new SqlSecurityException("只允许 SELECT 查询语句");
        }

        if (containsDdlOrDml(upperSql)) {
            throw new SqlSecurityException("禁止包含DDL/DML语句");
        }
        
        for (String keyword : DANGEROUS_KEYWORDS) {
            // 允许 UNION ALL（用于跨表查询），但禁止单独的 UNION
            if ("UNION".equals(keyword) && upperSql.contains("UNION ALL")) {
                continue;
            }
            if (upperSql.contains(keyword)) {
                log.warn("[validateSingle] 检测到危险关键字: {}, SQL: {}", keyword, sql);
                throw new SqlSecurityException("包含非法关键字: " + keyword);
            }
        }
        
        validateTableNames(sql);
        checkSubqueryDepth(sql);
        
        if (sql.length() > 5000) {
            throw new SqlSecurityException("SQL语句过长");
        }
        
        log.info("[validateSingle] SQL安全校验通过: {}", sql);
    }

    private boolean containsDdlOrDml(String upperSql) {
        String[] ddlDmlKeywords = {
            "DROP", "ALTER", "CREATE", "TRUNCATE", "RENAME",
            "INSERT", "UPDATE", "DELETE", "MERGE", "REPLACE",
            "GRANT", "REVOKE", "COMMIT", "ROLLBACK"
        };
        
        for (String keyword : ddlDmlKeywords) {
            if (upperSql.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private void validateTableNames(String sql) throws SqlSecurityException {
        String upperSql = sql.toUpperCase();
        Pattern tablePattern = Pattern.compile(
                "(?:FROM|JOIN|INTO|UPDATE)\s+([a-zA-Z_][a-zA-Z0-9_]*)",
                Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = tablePattern.matcher(upperSql);
        
        while (matcher.find()) {
            String tableName = matcher.group(1).toLowerCase();
            if (!ALLOWED_TABLES.contains(tableName)) {
                log.warn("检测到非法表名: {}, SQL: {}", tableName, sql);
                throw new SqlSecurityException("不允许访问表: " + tableName +
                        "。只允许访问: " + ALLOWED_TABLES);
            }
        }
    }
    
    private void checkSubqueryDepth(String sql) throws SqlSecurityException {
        int depth = 0;
        int maxDepth = 2;
        String upperSql = sql.toUpperCase();
        
        for (int i = 0; i < upperSql.length(); i++) {
            if (upperSql.startsWith("SELECT", i)) {
                depth++;
                if (depth > maxDepth) {
                    throw new SqlSecurityException("子查询嵌套深度超过限制（最多" + maxDepth + "层）");
                }
            }
        }
    }
    
    public String sanitize(String sql) {
        if (sql == null) return null;
        sql = sql.trim();
        sql = sql.replaceAll("--.*", "");
        sql = sql.replaceAll("/\\*.*?\\*/", "");
        sql = sql.replaceAll("/\\*!\\d+\\s+.*?\\*/", "");
        sql = sql.replaceAll("\\s+", " ");
        return sql;
    }
    
    public Set<String> getAllowedTables() {
        return new HashSet<>(ALLOWED_TABLES);
    }
}
