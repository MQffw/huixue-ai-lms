package com.itheima.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JWT配置类 - 支持密钥轮换
 */
@Configuration
public class JwtConfig {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration:43200000}") // 默认12小时
    private long expiration;
    
    @Value("${jwt.renewal-threshold:1800000}") // 默认30分钟
    private long renewalThreshold;
    
    // Getters
    public String getSecret() { return secret; }
    public long getExpiration() { return expiration; }
    public long getRenewalThreshold() { return renewalThreshold; }
}
