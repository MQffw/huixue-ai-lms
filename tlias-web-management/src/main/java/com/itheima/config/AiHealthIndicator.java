package com.itheima.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * AI 服务健康检测
 * 自定义 Actuator Health Indicator，检测各 AI 模型 API 的可达性
 */
@Component
public class AiHealthIndicator implements HealthIndicator {

    @Value("${ai.models.deepseek.api-url}") private String deepseekUrl;
    @Value("${ai.models.mimo.api-url}") private String mimoUrl;
    @Value("${ai.models.longcat.api-url}") private String longcatUrl;

    @Override
    public Health health() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            boolean deepseekOk = checkEndpoint(client, deepseekUrl);
            boolean mimoOk = checkEndpoint(client, mimoUrl);
            boolean longcatOk = checkEndpoint(client, longcatUrl);

            if (deepseekOk || mimoOk || longcatOk) {
                Health.Builder builder = Health.up()
                        .withDetail("deepseek", deepseekOk ? "UP" : "DOWN")
                        .withDetail("mimo", mimoOk ? "UP" : "DOWN")
                        .withDetail("longcat", longcatOk ? "UP" : "DOWN");
                return builder.build();
            }

            return Health.down()
                    .withDetail("deepseek", "DOWN")
                    .withDetail("mimo", "DOWN")
                    .withDetail("longcat", "DOWN")
                    .withDetail("error", "所有AI服务不可达")
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private boolean checkEndpoint(HttpClient client, String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            // 任何 HTTP 响应（包括 405/401）都说明服务可达
            return response.statusCode() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
