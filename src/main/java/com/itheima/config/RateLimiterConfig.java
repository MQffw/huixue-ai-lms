package com.itheima.config;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 限流配置 - 使用 Guava RateLimiter
 */
@Configuration
public class RateLimiterConfig {
    
    @Value("${rate-limiter.normal:100.0}")
    private double normalRate;
    
    @Value("${rate-limiter.ai:10.0}")
    private double aiRate;
    
    /**
     * 普通接口限流器 - 默认100次/秒
     */
    @Bean
    public RateLimiter normalRateLimiter() {
        return RateLimiter.create(normalRate);
    }
    
    /**
     * AI接口限流器 - 默认10次/秒
     */
    @Bean
    public RateLimiter aiRateLimiter() {
        return RateLimiter.create(aiRate);
    }
}
