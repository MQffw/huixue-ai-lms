package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 员工操作日志实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpLog {

    private Integer id;
    private Integer operateEmpId;   // 操作员工ID（对应表字段 operate_emp_id）
    private String className;       // 类名
    private String methodName;      // 方法名
    private String methodParams;    // 方法参数
    private String returnValue;     // 返回值
    private LocalDateTime operateTime; // 操作时间
    private Long costTime;          // 耗时（毫秒）
    private String info;            // 备注信息
    private String operateEmpName;  // 操作员工姓名（关联查询，非表字段）
}
