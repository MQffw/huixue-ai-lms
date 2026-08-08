package com.itheima.config;

import com.itheima.config.model.ModelConfig;
import com.itheima.config.model.ModelRegistryService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * AI 服务健康检测：基于模型注册表动态检测所有已启用模型 API 的可达性
 * （模型增删改后自动跟随，无需改代码）
 */
@Component
public class AiHealthIndicator implements HealthIndicator {

    private final ModelRegistryService modelRegistryService;

    public AiHealthIndicator(ModelRegistryService modelRegistryService) {
        this.modelRegistryService = modelRegistryService;
    }

    @Override
    public Health health() {
        List<ModelConfig> models = modelRegistryService.list();
        boolean anyUp = false;
        Health.Builder builder = Health.down();
        for (ModelConfig cfg : models) {
            boolean ok = checkEndpoint(cfg.getBaseUrl());
            anyUp = anyUp || ok;
            builder.withDetail(cfg.getType(), ok ? "UP" : "DOWN");
        }
        if (models.isEmpty()) {
            builder.withDetail("models", "none");
        }
        return anyUp ? builder.up().build() : builder.down().build();
    }

    private boolean checkEndpoint(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) return false;
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() < 500;
        } catch (Exception e) {
            return false;
        }
    }
}