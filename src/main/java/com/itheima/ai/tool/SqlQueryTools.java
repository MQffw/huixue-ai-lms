package com.itheima.ai.tool;

import com.itheima.security.sql.SecureSqlExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 只读 SQL 查询兜底工具
 *
 * 当固定业务工具无法覆盖用户问题时，模型可生成只读 SQL，
 * 经四层沙箱（表白名单 / 只读 / 危险关键字 / 子查询深度）校验后执行。
 * 校验失败返回 [安全限制] 提示，不会执行任何写操作。
 */
@Component
@RequiredArgsConstructor
public class SqlQueryTools {

    private final SecureSqlExecutor secureSqlExecutor;

    @Tool(description = """
            只读SQL查询兜底工具：当其他业务工具无法回答用户问题时使用。
            规则：
              - 只能执行 SELECT 查询，禁止任何写操作
              - 只能访问白名单表：emp, dept, student, clazz, emp_expr, course, course_schedule, attendance, exam, score, violation_log, payment, employment, notice, tlias_knowledge_doc, tlias_tool_call_log, tlias_token_usage
              - 最多返回 100 条记录，SQL 末尾请加 LIMIT 100
            返回：JSON 数组字符串；被拦截时返回 [安全限制] 说明
            示例：按性别查学员名单 → SELECT id, name, no, gender FROM student WHERE gender = 1 LIMIT 100
            """)
    public String executeReadOnlyQuery(String sql) {
        return secureSqlExecutor.executeQuery(sql);
    }
}