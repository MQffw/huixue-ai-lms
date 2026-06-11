package com.itheima.exception;

import com.itheima.pojo.Result;
import com.itheima.security.sql.SqlSecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    public Result handlerException(Exception e){
        log.error("程序出错了",e);
        return Result.error("出错了");
    }

    /**
     * 处理 SQL 安全异常
     */
    @ExceptionHandler
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result handlerSqlSecurityException(SqlSecurityException e){
        log.warn("SQL安全拦截: {}", e.getMessage());
        return Result.error("非法SQL操作: " + e.getMessage());
    }

    @ExceptionHandler
    public Result handlerDuplicateKeyException(DuplicateKeyException e){
        log.error("程序出错了",e);
        String message = e.getMessage();
        int i = message.indexOf("Duplicate entry");
        String errMsg = message.substring(i);
        String[] arr = errMsg.split(" ");
        return Result.error(arr[2]+"已存在");
    }
}
