package com.itheima.exception;

/**
 * AI服务异常
 */
public class AiServiceException extends RuntimeException {
    
    private String errorCode;
    
    public AiServiceException(String message) {
        super(message);
    }
    
    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public AiServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public AiServiceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
