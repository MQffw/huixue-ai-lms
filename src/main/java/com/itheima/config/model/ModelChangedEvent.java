package com.itheima.config.model;

import lombok.Getter;

/**
 * 模型配置变更事件：注册表更新后发布，ChatClientConfig 监听并失效对应缓存
 */
@Getter
public class ModelChangedEvent {
    private final String type;

    public ModelChangedEvent(String type) {
        this.type = type;
    }
}