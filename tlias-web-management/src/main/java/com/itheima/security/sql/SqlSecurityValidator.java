package com.itheima.security.sql;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SQL 安全校验器
 * 实现：SQL白名单、关键字黑名单、SQL注入防御
 */
@Slf4j
@Component
public class SqlSecurityValidator {

    // 允许访问的表名白名单
    private static final Set<String> ALLOWED_TABLES = new HashSet<>(Arrays.asList(
            "emp", "dept", "student", "clazz"
    ));

    // 危险关键字黑名单（SQL注入、数据导出等）
    private static final Set<String> DANGEROUS_KEYWORDS = new HashSet<>(Arrays.asList(
            "UNION", "INTO", "OUTFILE", "LOAD_FILE", "BENCHMARK",
            "SLEEP", "DELAY", "WAITFOR", "SHUTDOWN", "GRANT",
            "REVOKE", "CREATE", "ALTER", "DROP", "TRUNCATE",
            "DELETE", "UPDATE", "INSERT", "REPLACE", "MERGE",
            "EXEC", "EXECUTE", "CALL", "DECLARE", "CURSOR",
            "INFORMATION_SCHEMA", "MYSQL", "SYS", "PG_",
            "--", "/*", "*/", ";", "@@", "CHAR(", "CONCAT(",
            "0x", "HEX(", "UNHEX(", "ASCII(", "ORD("
    ));

    // SQL注入模式正则
    private static final Pattern[] INJECTION_PATTERNS = {
            Pattern.compile("('.+--)|(--)|(\\|)|(%7C)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\b(OR|AND)\\b\\s+\\d+\\s*=\\s*\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\b(OR|AND)\\b\\s+['\"].*['\"]\\s*=\\s*['\"].*['\"])", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(UNION\\s+(ALL\\s+)?SELECT)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(SELECT\\s+.*\\s+FROM\\s+.*\\s+WHERE\\s+.*\\s*=\\s*.*\\s*OR\\s+.*)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\bEXEC\\b|\\bEXECUTE\\b)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\bCHAR\\s*\\(\\s*\\d+\\s*\\))", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\bCONCAT\\s*\\()", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\b0x[0-9a-fA-F]+\\b)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\bLOAD_FILE\\s*\\()", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\bINTO\\s+OUTFILE\\b)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\bBENCHMARK\\s*\\()", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\bSLEEP\\s*\\()", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\bWAITFOR\\s+DELAY\\b)", Pattern.CASE_INSENSITIVE)
    };

    /**
     * 校验 SQL 语句的安全性
     * @param sql 待校验的 SQL 语句
     * @throws SqlSecurityException 如果 SQL 不安全
     */
    public void validate(String sql) throws SqlSecurityException {
        if (sql == null || sql.trim().isEmpty()) {
            throw new SqlSecurityException("SQL 语句不能为空");
        }

        String upperSql = sql.toUpperCase().trim();

        // 1. 必须以 SELECT 开头
        if (!upperSql.startsWith("SELECT")) {
            throw new SqlSecurityException("只允许 SELECT 查询语句");
        }

        // 2. 检查危险关键字
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (upperSql.contains(keyword)) {
                log.warn("检测到危险关键字: {}, SQL: {}", keyword, sql);
                throw new SqlSecurityException("包含非法关键字: " + keyword);
            }
        }

        // 3. 检查 SQL 注入模式
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(sql).find()) {
                log.warn("检测到 SQL 注入模式: {}, SQL: {}", pattern.pattern(), sql);
                throw new SqlSecurityException("检测到潜在的 SQL 注入攻击");
            }
        }

        // 4. 校验表名白名单
        validateTableNames(sql);

        // 5. 检查子查询嵌套深度（防止复杂查询导致性能问题）
        checkSubqueryDepth(sql);

        log.info("SQL 安全校验通过: {}", sql);
    }

    /**
     * 校验 SQL 中使用的表名是否在白名单中
     */
    private void validateTableNames(String sql) throws SqlSecurityException {
        // 提取 FROM 和 JOIN 后的表名
        Pattern tablePattern = Pattern.compile(
                "(?:FROM|JOIN)\\s+([a-zA-Z_][a-zA-Z0-9_]*)",
                Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = tablePattern.matcher(sql);

        while (matcher.find()) {
            String tableName = matcher.group(1).toLowerCase();
            if (!ALLOWED_TABLES.contains(tableName)) {
                log.warn("检测到非法表名: {}, SQL: {}", tableName, sql);
                throw new SqlSecurityException("不允许访问表: " + tableName +
                        "。只允许访问: " + ALLOWED_TABLES);
            }
        }
    }

    /**
     * 检查子查询嵌套深度（最多允许2层）
     */
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

    /**
     * 清理 SQL 语句（移除多余空格、换行等）
     */
    public String sanitize(String sql) {
        if (sql == null) return null;

        // 移除首尾空格
        sql = sql.trim();

        // 移除注释
        sql = sql.replaceAll("--.*", ""); // 单行注释
        sql = sql.replaceAll("/\\*.*?\\*/", ""); // 多行注释

        // 移除多余空白字符
        sql = sql.replaceAll("\\s+", " ");

        return sql;
    }

    /**
     * 获取允许的表名列表
     */
    public Set<String> getAllowedTables() {
        return new HashSet<>(ALLOWED_TABLES);
    }
}
