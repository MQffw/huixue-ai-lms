package com.itheima.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 当前用户上下文 - ThreadLocal管理
 * 防止内存泄漏和用户身份串号
 */
public class CurrentHolder {
    
    private static final Logger log = LoggerFactory.getLogger(CurrentHolder.class);
    
    private static final ThreadLocal<Integer> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_ROLE = new ThreadLocal<>();
    
    public static void setId(Integer id) { CURRENT_USER_ID.set(id); }
    public static Integer getId() { return CURRENT_USER_ID.get(); }
    public static void setUsername(String username) { CURRENT_USERNAME.set(username); }
    public static String getUsername() { return CURRENT_USERNAME.get(); }
    public static void setRole(String role) { CURRENT_ROLE.set(role); }
    public static String getRole() { return CURRENT_ROLE.get(); }
    
    /**
     * 强制清理所有ThreadLocal - 防止内存泄漏和身份串号
     */
    public static void remove() {
        try {
            CURRENT_USER_ID.remove();
            CURRENT_USERNAME.remove();
            CURRENT_ROLE.remove();
            log.debug("ThreadLocal清理完成");
        } catch (Exception e) {
            log.warn("ThreadLocal清理异常", e);
            forceClear();
        }
    }
    
    /**
     * 强制清除 - 最后的保障
     */
    private static void forceClear() {
        try {
            CURRENT_USER_ID.set(null);
            CURRENT_USERNAME.set(null);
            CURRENT_ROLE.set(null);
        } catch (Exception e) {
            log.error("ThreadLocal强制清除失败", e);
        }
    }
    
    /**
     * 检查是否有用户登录
     */
    public static boolean isAuthenticated() {
        return CURRENT_USER_ID.get() != null;
    }
    
    /**
     * 获取当前用户信息摘要（用于日志）
     */
    public static String getUserSummary() {
        Integer id = CURRENT_USER_ID.get();
        String username = CURRENT_USERNAME.get();
        return String.format("User[id=%s, username=%s]", id, username);
    }
}
