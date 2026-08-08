package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤记录实体
 * 对应表：attendance
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {
    private Integer id;
    private Integer studentId;
    private Integer clazzId;
    private LocalDate attendDate;
    private Integer status;        // 1:正常 2:迟到 3:早退 4:请假 5:旷课
    private String remark;
    private Integer recordEmpId;
    private LocalDateTime createTime;

    // 关联查询字段
    private String studentName;
    private String clazzName;
}
