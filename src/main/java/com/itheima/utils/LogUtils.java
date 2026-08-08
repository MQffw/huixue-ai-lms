package com.itheima.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * 日志工具类 - 自动脱敏敏感信息
 */
public class LogUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(LogUtils.class);
    
    /**
     * 安全打印实体 - 自动忽略 @JsonIgnore 字段
     */
    public static String toSafeString(Object obj) {
        if (obj == null) return "null";
        
        try {
            // 使用 Jackson 序列化（会自动忽略 @JsonIgnore 字段）
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            // 降级处理：只打印类名和ID
            return String.format("%s[id=%s]", 
                    obj.getClass().getSimpleName(), 
                    getFieldValue(obj, "id"));
        }
    }
    
    /**
     * 打印用户操作日志
     */
    public static void logUserAction(String action, Object... params) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户操作: ").append(action);
        if (params != null && params.length > 0) {
            sb.append(", 参数: [");
            for (int i = 0; i < params.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(toSafeString(params[i]));
            }
            sb.append("]");
        }
        log.info(sb.toString());
    }
    
    /**
     * 安全打印实体（排除敏感字段）
     */
    public static String toSafeStringExclude(Object obj, String... excludeFields) {
        if (obj == null) return "null";
        
        try {
            // 创建临时对象，将敏感字段设为null
            Object clone = obj.getClass().getDeclaredConstructor().newInstance();
            for (Field field : obj.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                boolean exclude = false;
                for (String excludeField : excludeFields) {
                    if (field.getName().equals(excludeField)) {
                        exclude = true;
                        break;
                    }
                }
                if (!exclude) {
                    field.set(clone, field.get(obj));
                }
            }
            return objectMapper.writeValueAsString(clone);
        } catch (Exception e) {
            return String.format("%s[id=%s]", 
                    obj.getClass().getSimpleName(), 
                    getFieldValue(obj, "id"));
        }
    }
    
    private static Object getFieldValue(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            return "unknown";
        }
    }
}
