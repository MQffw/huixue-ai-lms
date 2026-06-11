package com.itheima.security.sql;

import lombok.Getter;

/**
 * SQL 安全异常
 */
@Getter
public class SqlSecurityException extends RuntimeException {

    private final String sql;
    private final String violationType;

    public SqlSecurityException(String message) {
        super(message);
        this.sql = null;
        this.violationType = "SQL_SECURITY_VIOLATION";
    }

    public SqlSecurityException(String message, String sql) {
        super(message);
        this.sql = sql;
        this.violationType = "SQL_SECURITY_VIOLATION";
    }

    public SqlSecurityException(String message, Throwable cause) {
        super(message, cause);
        this.sql = null;
        this.violationType = "SQL_SECURITY_VIOLATION";
    }

    @Override
    public String toString() {
        return "SqlSecurityException{" +
                "violationType='" + violationType + '\'' +
                ", message='" + getMessage() + '\'' +
                ", sql='" + (sql != null ? sql : "N/A") + '\'' +
                '}';
    }
}
