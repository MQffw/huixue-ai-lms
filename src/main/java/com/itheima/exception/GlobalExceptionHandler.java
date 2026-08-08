package com.itheima.exception;

import com.itheima.pojo.Result;
import com.itheima.security.sql.SqlSecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器 - 分层处理，统一错误码
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 业务异常码枚举
     */
    public enum ErrorCode {
        SUCCESS(200, "success"),
        BAD_REQUEST(400, "参数错误"),
        UNAUTHORIZED(401, "未登录"),
        FORBIDDEN(403, "无权限"),
        NOT_FOUND(404, "资源不存在"),
        METHOD_NOT_ALLOWED(405, "请求方法不支持"),
        REQUEST_TIMEOUT(408, "请求超时"),
        TOO_MANY_REQUESTS(429, "请求过于频繁"),
        INTERNAL_ERROR(500, "服务内部错误"),
        DATABASE_ERROR(501, "数据库操作失败"),
        AI_SERVICE_ERROR(502, "AI服务调用失败"),
        EXTERNAL_SERVICE_ERROR(503, "外部服务调用失败");
        
        private final int code;
        private final String message;
        
        ErrorCode(int code, String message) {
            this.code = code;
            this.message = message;
        }
        
        public int getCode() { return code; }
        public String getMessage() { return message; }
    }
    
    /**
     * 参数校验异常 - 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleValidation(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String errorMessage = fieldErrors.stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("参数校验失败: {}", errorMessage);
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), errorMessage);
    }
    
    /**
     * 绑定异常 - 400
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleBind(BindException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("参数绑定失败: {}", errorMessage);
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), errorMessage);
    }
    
    /**
     * SQL安全异常 - 403
     */
    @ExceptionHandler(SqlSecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result handleSqlSecurity(SqlSecurityException e) {
        log.warn("SQL安全拦截: {}", e.getMessage());
        return Result.error(ErrorCode.FORBIDDEN.getCode(), "非法SQL操作: " + e.getMessage());
    }
    
    /**
     * 唯一键冲突异常 - 400
     */
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleDuplicateKey(DuplicateKeyException e) {
        String message = e.getMessage();
        String fieldName = extractDuplicateField(message);
        log.warn("唯一键冲突: {}", message);
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), fieldName + "已存在");
    }
    
    /**
     * 数据库异常 - 501（不暴露详细信息）
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleDataAccess(DataAccessException e) {
        log.error("数据库异常: {}", e.getMessage(), e); // 内部日志记录完整信息
        return Result.error(ErrorCode.DATABASE_ERROR.getCode(), "数据库操作失败，请稍后重试");
    }
    
    /**
     * AI服务异常 - 502
     */
    @ExceptionHandler(AiServiceException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Result handleAiService(AiServiceException e) {
        log.error("AI服务异常: {}", e.getMessage(), e);
        return Result.error(ErrorCode.AI_SERVICE_ERROR.getCode(), "AI服务暂时不可用，请稍后重试");
    }
    
    /**
     * SQL异常 - 501
     */
    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleSql(SQLException e) {
        log.error("SQL异常: {}", e.getMessage(), e);
        return Result.error(ErrorCode.DATABASE_ERROR.getCode(), "数据库操作失败");
    }
    
    /**
     * 静态资源不存在 - 404（不记录日志，避免 favicon.ico 噪音）
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result handleNoResource(org.springframework.web.servlet.resource.NoResourceFoundException e) {
        return Result.error(404, "资源不存在");
    }

    /**
     * Prompt Injection 攻击拦截 - 403
     */
    @ExceptionHandler(com.itheima.ai.advisor.SafeGuardAdvisor.PromptInjectionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result handlePromptInjection(com.itheima.ai.advisor.SafeGuardAdvisor.PromptInjectionException e) {
        log.warn("Prompt Injection攻击被拦截: {}", e.getMessage());
        return Result.error(ErrorCode.FORBIDDEN.getCode(), "输入包含不安全的指令，已被安全拦截");
    }

    /**
     * 其他异常 - 500（不暴露堆栈信息）
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e); // 内部日志记录完整信息
        return Result.error(ErrorCode.INTERNAL_ERROR.getCode(), "系统繁忙，请稍后重试");
    }
    
    /**
     * 提取重复键字段名
     */
    private String extractDuplicateField(String message) {
        int i = message.indexOf("Duplicate entry");
        if (i == -1) return "数据";
        try {
            String sub = message.substring(i);
            String[] arr = sub.split(" ");
            return arr.length > 2 ? arr[2] : "数据";
        } catch (Exception e) {
            return "数据";
        }
    }
}
