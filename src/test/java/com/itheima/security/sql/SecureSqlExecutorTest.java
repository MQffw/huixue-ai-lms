package com.itheima.security.sql;

import com.itheima.TliasWebManagementApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全SQL执行器集成测试
 */
@SpringBootTest(classes = TliasWebManagementApplication.class)
@ActiveProfiles("test")
class SecureSqlExecutorTest {

    @Autowired
    private SecureSqlExecutor secureSqlExecutor;

    @Test
    @DisplayName("测试执行合法查询")
    void testExecuteValidQuery() {
        String sql = "SELECT * FROM dept";
        String result = secureSqlExecutor.executeQuery(sql);

        assertNotNull(result);
        assertFalse(result.startsWith("[安全限制]"));
        assertFalse(result.startsWith("[查询失败]"));
        System.out.println("查询结果: " + result);
    }

    @Test
    @DisplayName("测试执行带参数查询")
    void testExecuteQueryWithParams() {
        String sql = "SELECT * FROM emp WHERE dept_id = ?";
        List<Object> params = Arrays.asList(1);
        String result = secureSqlExecutor.executeQuery(sql, params);

        assertNotNull(result);
        assertFalse(result.startsWith("[安全限制]"));
        System.out.println("带参数查询结果: " + result);
    }

    @Test
    @DisplayName("测试执行命名参数查询")
    void testExecuteNamedQuery() {
        String sql = "SELECT * FROM student WHERE gender = :gender";
        Map<String, Object> params = new HashMap<>();
        params.put("gender", 1);
        String result = secureSqlExecutor.executeNamedQuery(sql, params);

        assertNotNull(result);
        assertFalse(result.startsWith("[安全限制]"));
        System.out.println("命名参数查询结果: " + result);
    }

    @Test
    @DisplayName("测试拦截危险操作")
    void testBlockDangerousOperations() {
        String[] dangerousQueries = {
                "DROP TABLE emp",
                "DELETE FROM emp WHERE id = 1",
                "UPDATE emp SET name = 'test'",
                "INSERT INTO emp (name) VALUES ('test')"
        };

        for (String sql : dangerousQueries) {
            String result = secureSqlExecutor.executeQuery(sql);
            assertTrue(result.startsWith("[安全限制]"),
                    "应该拦截危险操作: " + sql);
            System.out.println("已拦截: " + sql + " -> " + result);
        }
    }

    @Test
    @DisplayName("测试拦截非法表访问")
    void testBlockIllegalTableAccess() {
        String sql = "SELECT * FROM users";
        String result = secureSqlExecutor.executeQuery(sql);

        assertTrue(result.startsWith("[安全限制]"));
        assertTrue(result.contains("不允许访问表"));
        System.out.println("已拦截非法表访问: " + result);
    }

    @Test
    @DisplayName("测试拦截SQL注入")
    void testBlockSqlInjection() {
        String[] injectionQueries = {
                "SELECT * FROM emp WHERE name = '' OR 1=1",
                "SELECT * FROM emp UNION SELECT 1,2,3",
                "SELECT * FROM emp; DROP TABLE emp"
        };

        for (String sql : injectionQueries) {
            String result = secureSqlExecutor.executeQuery(sql);
            assertTrue(
                    result.startsWith("[安全限制]") || result.startsWith("[查询失败]"),
                    "应该拦截SQL注入: " + sql
            );
            System.out.println("已拦截SQL注入: " + sql + " -> " + result);
        }
    }

    @Test
    @DisplayName("测试获取允许的表列表")
    void testGetAllowedTables() {
        List<String> tables = secureSqlExecutor.getAllowedTables();

        assertNotNull(tables);
        assertTrue(tables.size() >= 4);
        assertTrue(tables.contains("emp"));
        assertTrue(tables.contains("dept"));
        assertTrue(tables.contains("student"));
        assertTrue(tables.contains("clazz"));

        System.out.println("允许的表: " + tables);
    }

    @Test
    @DisplayName("测试统计查询")
    void testCountQuery() {
        String sql = "SELECT COUNT(*) as total FROM student";
        String result = secureSqlExecutor.executeQuery(sql);

        assertNotNull(result);
        assertFalse(result.startsWith("[安全限制]"));
        System.out.println("统计查询结果: " + result);
    }

    @Test
    @DisplayName("测试分组查询")
    void testGroupByQuery() {
        String sql = "SELECT gender, COUNT(*) as count FROM student GROUP BY gender";
        String result = secureSqlExecutor.executeQuery(sql);

        assertNotNull(result);
        assertFalse(result.startsWith("[安全限制]"));
        System.out.println("分组查询结果: " + result);
    }
}
