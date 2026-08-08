package com.itheima.config.model;

import lombok.Data;

/**
 * 模型配置项（模型注册表条目）
 * apiKey 支持 ${ENV_NAME} 占位符，运行时从环境变量解析，避免明文入库
 */
@Data
public class ModelConfig {
    private String type;          // 模型唯一标识，如 deepseek / qwen
    private String name;          // 展示名称
    private String baseUrl;       // OpenAI 兼容接口根地址（不含 /v1）
    private String apiKey;        // 密钥，或 ${ENV_NAME} 环境变量占位
    private String model;         // 模型名
    private Double temperature = 0.3;
    private Integer maxTokens = 1024;
    private Boolean enabled = true;
}