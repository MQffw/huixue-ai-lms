package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知公告实体
 * 对应表：notice
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notice {
    private Integer id;
    private String title;
    private String content;
    private Integer type;           // 1:通知 2:公告 3:制度
    private String targetAudience;  // 目标受众: 全体/教师/学员/某班级
    private Integer publishEmpId;
    private LocalDateTime publishTime;
    private Integer isTop;          // 是否置顶: 0:否 1:是
    private LocalDateTime createTime;

    // 关联查询字段
    private String publisherName;
}
