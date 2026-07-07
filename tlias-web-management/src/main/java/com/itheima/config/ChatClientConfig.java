package com.itheima.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多模型 ChatClient 工厂 + 模型路由
 *
 * 职责：
 * 1. 管理 DeepSeek / Mimo / LongCat 三种模型的 ChatClient 实例（带缓存）
 * 2. 按意图类型返回合适的 maxTokens / temperature 配置
 *
 * 注意：系统 Prompt 已迁移到 AiPromptBuilder + prompts/*.txt，不再在此处硬编码。
 */
@Configuration
public class ChatClientConfig {

    @Value("${ai.models.deepseek.api-key}")  private String deepseekKey;
    @Value("${ai.models.deepseek.api-url}")  private String deepseekUrl;
    @Value("${ai.models.deepseek.model}")    private String deepseekModel;

    @Value("${ai.models.mimo.api-key}")  private String mimoKey;
    @Value("${ai.models.mimo.api-url}")  private String mimoUrl;
    @Value("${ai.models.mimo.model}")    private String mimoModel;

    @Value("${ai.models.longcat.api-key}")  private String longcatKey;
    @Value("${ai.models.longcat.api-url}")  private String longcatUrl;
    @Value("${ai.models.longcat.model}")    private String longcatModel;

    @Value("${ai.max-tokens:1024}")         private int defaultMaxTokens;
    @Value("${ai.max-tokens-chat:1024}")    private int chatMaxTokens;
    @Value("${ai.max-tokens-tool:2048}")    private int toolMaxTokens;
    @Value("${ai.temperature:0.3}")         private double temperature;

    private final Map<String, ChatClient> chatClientCache = new ConcurrentHashMap<>();

    public ChatClient getChatClient(String modelType) {
        if (modelType == null) modelType = "longcat";
        String key = modelType.toLowerCase();
        return chatClientCache.computeIfAbsent(key, k -> {
            ChatModel chatModel = createChatModel(k);
            return ChatClient.builder(chatModel).build();
        });
    }

    /**
     * 按意图类型返回对应的 maxTokens 配置
     * @param toolMode true 表示本轮期望触发工具调用（给更多空间放工具结果）
     */
    public int resolveMaxTokens(boolean toolMode) {
        return toolMode ? Math.max(toolMaxTokens, defaultMaxTokens) : chatMaxTokens;
    }

    private ChatModel createChatModel(String modelType) {
        String apiKey, apiUrl, modelName;
        switch (modelType) {
            case "deepseek" -> { apiKey = deepseekKey; apiUrl = deepseekUrl; modelName = deepseekModel; }
            case "mimo"     -> { apiKey = mimoKey;     apiUrl = mimoUrl;     modelName = mimoModel; }
            default         -> { apiKey = longcatKey;  apiUrl = longcatUrl;  modelName = longcatModel; }
        }
        String baseUrl = apiUrl;
        if (baseUrl.endsWith("/v1/chat/completions")) baseUrl = baseUrl.substring(0, baseUrl.length() - "/v1/chat/completions".length());
        else if (baseUrl.endsWith("/v1")) baseUrl = baseUrl.substring(0, baseUrl.length() - "/v1".length());
        return OpenAiChatModel.builder().openAiApi(
            OpenAiApi.builder().baseUrl(baseUrl).apiKey(apiKey).build())
            .defaultOptions(OpenAiChatOptions.builder().model(modelName).temperature(temperature).build()).build();
    }
}
