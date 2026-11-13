package com.huixuetong.ai;

import com.huixuetong.config.ChatClientConfig;
import com.huixuetong.config.model.ModelConfig;
import com.huixuetong.config.model.ModelRegistryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 默认模型解析：不再硬编码 longcat，改为取注册表第一个可用模型。
 */
class ChatClientConfigTest {

    private ModelConfig model(String type) {
        ModelConfig c = new ModelConfig();
        c.setType(type);
        c.setBaseUrl("https://api.example.com");
        c.setModel("m-1");
        return c;
    }

    @Test
    @DisplayName("默认模型取注册表第一个")
    void defaultModelIsFirstRegistered() {
        ModelRegistryService registry = mock(ModelRegistryService.class);
        when(registry.list()).thenReturn(List.of(model("deepseek"), model("mimo")));

        assertEquals("deepseek", new ChatClientConfig(registry).defaultModelType());
    }

    @Test
    @DisplayName("注册表为空时给出明确错误")
    void emptyRegistryThrows() {
        ModelRegistryService registry = mock(ModelRegistryService.class);
        when(registry.list()).thenReturn(List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new ChatClientConfig(registry).defaultModelType());
        assertTrue(ex.getMessage().contains("模型注册表为空"));
    }
}
