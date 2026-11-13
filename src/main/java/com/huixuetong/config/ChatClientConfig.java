package com.huixuetong.config;

import com.huixuetong.config.model.ModelChangedEvent;
import com.huixuetong.config.model.ModelConfig;
import com.huixuetong.config.model.ModelRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多模型 ChatClient 工厂（模型注册表驱动）
 *
 * 职责：
 * 1. 从 ModelRegistryService 读取模型配置（config/models.json），按需构建 ChatClient
 * 2. 缓存 ChatClient 实例；模型配置变更时通过 ModelChangedEvent 失效对应缓存，下次请求懒重建
 * 3. 新增模型无需改代码：注册表里加一条配置即可
 */
@Component
@RequiredArgsConstructor
public class ChatClientConfig {

    private final ModelRegistryService modelRegistryService;

    private final Map<String, ChatClient> chatClientCache = new ConcurrentHashMap<>();

    public ChatClient getChatClient(String modelType) {
        String type = (modelType == null || modelType.trim().isEmpty())
                ? defaultModelType()
                : modelType.trim().toLowerCase();
        return chatClientCache.computeIfAbsent(type, k -> {
            ModelConfig cfg = modelRegistryService.get(type);
            if (cfg == null) {
                throw new IllegalArgumentException("模型不存在或未启用: " + type);
            }
            return ChatClient.builder(createChatModel(cfg)).build();
        });
    }

    /**
     * 默认模型：取注册表里第一个可用模型。
     * 不再硬编码 longcat —— 注册表里本来就没有它，硬编码会导致"先抛异常再降级"。
     */
    public String defaultModelType() {
        var models = modelRegistryService.list();
        if (models == null || models.isEmpty()) {
            throw new IllegalStateException("模型注册表为空，请先在 /ai/models 配置至少一个可用模型");
        }
        return models.get(0).getType();
    }

    /** 失效某个模型的缓存（模型配置更新/删除后调用） */
    public void evict(String modelType) {
        if (modelType != null) {
            chatClientCache.remove(modelType.trim().toLowerCase());
        }
    }

    @EventListener
    public void onModelChanged(ModelChangedEvent event) {
        // 模型表很小，直接全清：默认模型可能被删除/改名，残留缓存会指向旧配置
        chatClientCache.clear();
    }

    private ChatModel createChatModel(ModelConfig cfg) {
        String baseUrl = normalizeBaseUrl(cfg.getBaseUrl());
        return OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(baseUrl)
                        .apiKey(modelRegistryService.resolveApiKey(cfg))
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(cfg.getModel())
                        .temperature(cfg.getTemperature() != null ? cfg.getTemperature() : 0.3)
                        .maxTokens(cfg.getMaxTokens() != null ? cfg.getMaxTokens() : 1024)
                        .build())
                .build();
    }

    /** 兼容用户粘贴完整地址（/v1 或 /v1/chat/completions）的情况 */
    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) return baseUrl;
        if (baseUrl.endsWith("/v1/chat/completions")) {
            return baseUrl.substring(0, baseUrl.length() - "/v1/chat/completions".length());
        }
        if (baseUrl.endsWith("/v1")) {
            return baseUrl.substring(0, baseUrl.length() - "/v1".length());
        }
        return baseUrl;
    }
}