package com.itheima.security.sql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SQL安全校验器单元测试
 */
class SqlSecurityValidatorTest {

    private SqlSecurityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SqlSecurityValidator();
    }

    @Test
    @DisplayName("测试合法SELECT查询 - emp表")
    void testValidSelectFromEmp() {
        String sql = "SELECT * FROM emp";
        assertDoesNotThrow(() -> validator.validate(sql));
    }

    @Test
    @DisplayName("测试合法SELECT查询 - dept表")
    void testValidSelectFromDept() {
        String sql = "SELECT name FROM dept WHERE id = 1";
        assertDoesNotThrow(() -> validator.validate(sql));
    }

    @Test
    @DisplayName("测试合法SELECT查询 - student表")
    void testValidSelectFromStudent() {
        String sql = "SELECT COUNT(*) as total FROM student";
        assertDoesNotThrow(() -> validator.validate(sql));
    }

    @Test
    @DisplayName("测试合法SELECT查询 - clazz表")
    void testValidSelectFromClazz() {
        String sql = "SELECT name, room FROM clazz WHERE subject = 'Java'";
        assertDoesNotThrow(() -> validator.validate(sql));
    }

    @Test
    @DisplayName("测试合法JOIN查询")
    void testValidJoinQuery() {
        String sql = "SELECT e.name, d.name as dept_name FROM emp e JOIN dept d ON e.dept_id = d.id";
        assertDoesNotThrow(() -> validator.validate(sql));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "DROP TABLE emp",
            "ALTER TABLE emp ADD COLUMN test VARCHAR(50)",
            "DELETE FROM emp WHERE id = 1",
            "UPDATE emp SET name = 'test' WHERE id = 1",
            "INSERT INTO emp (name) VALUES ('test')",
            "TRUNCATE TABLE emp"
    })
    @DisplayName("测试拦截危险操作")
    void testBlockDangerousOperations(String sql) {
        SqlSecurityException exception = assertThrows(
                SqlSecurityException.class,
                () -> validator.validate(sql)
        );
        assertTrue(exception.getMessage().contains("只允许 SELECT"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * FROM users",
            "SELECT * FROM information_schema.tables",
            "SELECT * FROM mysql.user",
            "SELECT * FROM sys.objects"
    })
    @DisplayName("测试拦截非法表访问")
    void testBlockIllegalTableAccess(String sql) {
        SqlSecurityException exception = assertThrows(
                SqlSecurityException.class,
                () -> validator.validate(sql)
        );
        assertTrue(exception.getMessage().contains("不允许访问表"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * FROM emp UNION SELECT * FROM dept",
            "SELECT * FROM emp WHERE name = '' OR 1=1",
            "SELECT * FROM emp; DROP TABLE emp",
            "SELECT * FROM emp WHERE id = 1 UNION SELECT 1,2,3",
            "SELECT LOAD_FILE('/etc/passwd')",
            "SELECT * FROM emp INTO OUTFILE '/tmp/test.txt'",
            "SELECT BENCHMARK(10000000, MD5('test'))",
            "SELECT SLEEP(5)",
            "SELECT * FROM emp -- comment",
            "SELECT * FROM emp /* comment */"
    })
    @DisplayName("测试拦截SQL注入攻击")
    void testBlockSqlInjection(String sql) {
        SqlSecurityException exception = assertThrows(
                SqlSecurityException.class,
                () -> validator.validate(sql)
        );
        assertTrue(
                exception.getMessage().contains("非法关键字") ||
                exception.getMessage().contains("SQL 注入") ||
                exception.getMessage().contains("非法表") ||
                exception.getMessage().contains("只允许 SELECT"),
                "Expected security violation for: " + sql
        );
    }

    @Test
    @DisplayName("测试空SQL")
    void testEmptySql() {
        SqlSecurityException exception = assertThrows(
                SqlSecurityException.class,
                () -> validator.validate("")
        );
        assertTrue(exception.getMessage().contains("不能为空"));
    }

    @Test
    @DisplayName("测试null SQL")
    void testNullSql() {
        SqlSecurityException exception = assertThrows(
                SqlSecurityException.class,
                () -> validator.validate(null)
        );
        assertTrue(exception.getMessage().contains("不能为空"));
    }

    @Test
    @DisplayName("测试SQL清理")
    void testSanitize() {
        String sql = "  SELECT * FROM emp   WHERE id = 1  ";
        String sanitized = validator.sanitize(sql);
        assertEquals("SELECT * FROM emp WHERE id = 1", sanitized);
    }

    @Test
    @DisplayName("测试获取允许的表列表")
    void testGetAllowedTables() {
        assertEquals(4, validator.getAllowedTables().size());
        assertTrue(validator.getAllowedTables().contains("emp"));
        assertTrue(validator.getAllowedTables().contains("dept"));
        assertTrue(validator.getAllowedTables().contains("student"));
        assertTrue(validator.getAllowedTables().contains("clazz"));
    }

    @Test
    @DisplayName("测试复杂合法查询")
    void testComplexValidQuery() {
        String sql = "SELECT e.name, COUNT(*) as count FROM emp e " +
                     "JOIN dept d ON e.dept_id = d.id " +
                     "WHERE e.salary > 5000 " +
                     "GROUP BY e.name " +
                     "ORDER BY count DESC " +
                     "LIMIT 10";
        assertDoesNotThrow(() -> validator.validate(sql));
    }
}
