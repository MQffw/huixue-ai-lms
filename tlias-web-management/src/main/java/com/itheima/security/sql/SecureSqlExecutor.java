package com.itheima.security.sql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 安全的 SQL 执行器
 * 使用预编译查询防止 SQL 注入
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecureSqlExecutor {

    private final JdbcTemplate jdbcTemplate;
    private final SqlSecurityValidator securityValidator;
    private final ObjectMapper objectMapper;

    /**
     * 安全执行查询（返回List格式）
     */
    public String executeQuery(String sql) {
        return executeQuery(sql, Collections.emptyList());
    }

    /**
     * 安全执行带参数的查询（预编译）
     * @param sql SQL语句（使用 ? 作为占位符）
     * @param params 参数列表（按顺序）
     */
    public String executeQuery(String sql, List<Object> params) {
        try {
            // 1. 清理 SQL
            String sanitizedSql = securityValidator.sanitize(sql);

            // 2. 安全校验
            securityValidator.validate(sanitizedSql);

            // 3. 提取表名并验证
            String tableName = extractMainTable(sanitizedSql);
            log.info("执行安全查询: {}, 表: {}, 参数: {}", sanitizedSql, tableName, params);

            // 4. 使用预编译查询
            List<Map<String, Object>> results;
            if (params.isEmpty()) {
                results = jdbcTemplate.queryForList(sanitizedSql);
            } else {
                results = jdbcTemplate.queryForList(sanitizedSql, params.toArray());
            }

            // 5. 处理结果
            if (results == null || results.isEmpty()) {
                return "查询结果为空";
            }

            return objectMapper.writeValueAsString(results);

        } catch (SqlSecurityException e) {
            log.warn("SQL安全拦截: {}", e.getMessage());
            return "[安全限制] " + e.getMessage();
        } catch (DataAccessException e) {
            log.error("数据库查询失败: {}, SQL: {}", e.getMessage(), sql, e);
            return "[查询失败] " + e.getMostSpecificCause().getMessage();
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败: {}", e.getMessage(), e);
            return "[结果处理失败] 查询成功但无法序列化结果";
        } catch (Exception e) {
            log.error("查询异常: {}, SQL: {}", e.getMessage(), sql, e);
            return "[查询失败] " + e.getMessage();
        }
    }

    /**
     * 安全执行带命名参数的查询（预编译）
     * @param sql SQL语句（使用 :paramName 作为占位符）
     * @param paramMap 参数映射
     */
    public String executeNamedQuery(String sql, Map<String, Object> paramMap) {
        try {
            // 1. 清理 SQL
            String sanitizedSql = securityValidator.sanitize(sql);

            // 2. 安全校验
            securityValidator.validate(sanitizedSql);

            // 3. 提取表名并验证
            String tableName = extractMainTable(sanitizedSql);
            log.info("执行安全查询: {}, 表: {}, 参数: {}", sanitizedSql, tableName, paramMap);

            // 4. 使用 NamedParameterJdbcTemplate 进行预编译查询
            org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate namedTemplate =
                    new org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(jdbcTemplate);

            List<Map<String, Object>> results = namedTemplate.queryForList(sanitizedSql, paramMap);

            // 5. 处理结果
            if (results == null || results.isEmpty()) {
                return "查询结果为空";
            }

            return objectMapper.writeValueAsString(results);

        } catch (SqlSecurityException e) {
            log.warn("SQL安全拦截: {}", e.getMessage());
            return "[安全限制] " + e.getMessage();
        } catch (DataAccessException e) {
            log.error("数据库查询失败: {}, SQL: {}", e.getMessage(), sql, e);
            return "[查询失败] " + e.getMostSpecificCause().getMessage();
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败: {}", e.getMessage(), e);
            return "[结果处理失败] 查询成功但无法序列化结果";
        } catch (Exception e) {
            log.error("查询异常: {}, SQL: {}", e.getMessage(), sql, e);
            return "[查询失败] " + e.getMessage();
        }
    }

    /**
     * 从 SQL 语句中提取主表名
     */
    private String extractMainTable(String sql) {
        Pattern fromPattern = Pattern.compile("FROM\\s+([a-zA-Z_][a-zA-Z0-9_]*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = fromPattern.matcher(sql);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknown";
    }

    /**
     * 获取允许的表名列表
     */
    public List<String> getAllowedTables() {
        return List.copyOf(securityValidator.getAllowedTables());
    }
}
