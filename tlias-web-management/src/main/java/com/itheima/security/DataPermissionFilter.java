package com.itheima.security;

import com.itheima.utils.CurrentHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 数据权限过滤器
 * 当前系统仅管理员角色，校验用户是否已登录即可
 */
@Component
@Slf4j
public class DataPermissionFilter {

    /**
     * 检查当前用户是否已登录
     */
    public boolean isLoggedIn() {
        return CurrentHolder.getId() != null;
    }

    /**
     * 检查是否可以访问指定班级
     */
    public boolean canAccessClazz(Integer clazzId) {
        return isLoggedIn();
    }

    /**
     * 检查是否可以访问指定学生
     */
    public boolean canAccessStudent(Integer studentId) {
        return isLoggedIn();
    }
}
