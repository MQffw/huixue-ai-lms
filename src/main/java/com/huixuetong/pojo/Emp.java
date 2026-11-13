package com.huixuetong.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.huixuetong.utils.PhoneMaskSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Emp {
    
    private Integer id;
    private String username;
    
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    
    private String name;
    private Integer gender;

    // 只在 JSON 输出时脱敏；getter 保持返回真实号码，避免 MyBatis 把脱敏值写回数据库
    @JsonSerialize(using = PhoneMaskSerializer.class)
    private String phone;

    private Integer job;
    private Integer salary;  // 数据库是int，用Integer兼容
    private String image;
    private LocalDate entryDate;
    private Integer deptId;
    private String deptName;        // 部门名称（关联查询，非表字段）
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
