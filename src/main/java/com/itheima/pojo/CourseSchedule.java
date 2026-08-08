package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 排课实体
 * 对应表：course_schedule
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseSchedule {
    private Integer id;
    private Integer clazzId;
    private Integer courseId;
    private Integer teacherId;
    private LocalDate classDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    private LocalDateTime createTime;

    // 关联查询字段
    private String clazzName;
    private String courseName;
    private String teacherName;
}
