package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考试实体
 * 对应表：exam
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Exam {
    private Integer id;
    private String name;
    private Integer clazzId;
    private Integer courseId;
    private LocalDate examDate;
    private Integer fullScore;     // 满分
    private Integer passScore;     // 及格分
    private LocalDateTime createTime;

    // 关联查询字段
    private String clazzName;
    private String courseName;
}
