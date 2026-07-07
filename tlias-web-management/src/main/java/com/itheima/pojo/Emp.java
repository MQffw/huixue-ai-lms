package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String phone;
    private String idCard;
    private Integer job;
    private Integer salary;  // 数据库是int，用Integer兼容
    private String image;
    private LocalDate entryDate;
    private Integer deptId;
    private String deptName;        // 部门名称（关联查询，非表字段）
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // 手机号脱敏
    public String getPhone() {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
    
    // 身份证脱敏
    public String getIdCard() {
        if (idCard == null || idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }
}
