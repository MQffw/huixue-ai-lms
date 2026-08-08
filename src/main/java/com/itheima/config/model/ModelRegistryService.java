package com.itheima.config.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型注册表服务
 * - 配置来源：外部文件（默认 config/models.json，可用 ai.models-file 覆盖）
 * - 文件不存在时用 models.json.example 模板初始化并落盘
 * - 支持运行时增删改：save/delete 后立即生效（通过 ModelChangedEvent 通知 ChatClientConfig 重建）
 * - apiKey 支持 ${ENV_NAME} 占位符，从环境变量解析，密钥不进 git
 */
@Slf4j
@Service
public class ModelRegistryService {

    @Value("${ai.models-file:config/models.json}")
    private String modelsFilePath;

    private final ApplicationEventPublisher eventPublisher;
    private final ModelKeyCipher keyCipher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** type -> 模型配置（仅 enabled） */
    private final Map<String, ModelConfig> registry = new ConcurrentHashMap<>();

    public ModelRegistryService(ApplicationEventPublisher eventPublisher, ModelKeyCipher keyCipher) {
        this.eventPublisher = eventPublisher;
        this.keyCipher = keyCipher;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    public synchronized void reload() {
        try {
            Path path = resolvePath();
            if (!Files.exists(path)) {
                seedFromExample(path);
            }
            ModelRegistry root = objectMapper.readValue(Files.readString(path, StandardCharsets.UTF_8), ModelRegistry.class);
            registry.clear();
            if (root.getModels() != null) {
                for (ModelConfig cfg : root.getModels()) {
                    if (cfg.getType() == null || cfg.getType().trim().isEmpty()) continue;
                    if (Boolean.FALSE.equals(cfg.getEnabled())) continue;
                    if (cfg.getBaseUrl() == null || cfg.getModel() == null) continue;
                    registry.put(cfg.getType().trim().toLowerCase(), cfg);
                }
            }
            log.info("模型注册表加载完成: 共 {} 个可用模型, 文件: {}", registry.size(), path.toAbsolutePath());
        } catch (Exception e) {
            log.error("模型注册表加载失败: {}", e.getMessage(), e);
        }
    }

    public List<ModelConfig> list() {
        List<ModelConfig> result = new ArrayList<>(registry.values());
        result.sort(Comparator.comparing(ModelConfig::getType));
        return result;
    }

    public ModelConfig get(String type) {
        return type == null ? null : registry.get(type.trim().toLowerCase());
    }

    /** 新增或更新模型；更新后写盘并通知重建 ChatClient */
    public synchronized ModelConfig save(ModelConfig cfg) {
        if (cfg.getType() == null || cfg.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("模型 type 不能为空");
        }
        if (!StringUtils.hasText(cfg.getBaseUrl()) || !StringUtils.hasText(cfg.getModel())) {
            throw new IllegalArgumentException("baseUrl 和 model 不能为空");
        }
        cfg.setType(cfg.getType().trim().toLowerCase());
        if (cfg.getTemperature() == null) cfg.setTemperature(0.3);
        if (cfg.getMaxTokens() == null) cfg.setMaxTokens(1024);
        if (cfg.getEnabled() == null) cfg.setEnabled(true);

        // 明文 key 自动加密落盘；${ENV} 与 enc: 原样保留
        cfg.setApiKey(keyCipher.encrypt(cfg.getApiKey()));

        ModelRegistry root = readFileSafe();
        ModelConfig target = cfg;
        for (ModelConfig m : root.getModels()) {
            if (m.getType().equalsIgnoreCase(cfg.getType())) {
                m.setName(cfg.getName());
                m.setBaseUrl(cfg.getBaseUrl());
                m.setModel(cfg.getModel());
                m.setTemperature(cfg.getTemperature());
                m.setMaxTokens(cfg.getMaxTokens());
                m.setEnabled(cfg.getEnabled());
                // apiKey 留空表示不修改（保留原密钥）
                if (cfg.getApiKey() != null && !cfg.getApiKey().isEmpty()) {
                    m.setApiKey(cfg.getApiKey());
                }
                target = m;
                break;
            }
        }
        if (target == cfg) {
            root.getModels().add(cfg);
        }
        writeFile(root);

        if (Boolean.TRUE.equals(target.getEnabled())) {
            registry.put(target.getType(), target);
        } else {
            registry.remove(target.getType());
        }
        eventPublisher.publishEvent(new ModelChangedEvent(target.getType()));
        log.info("模型配置已更新: type={}, name={}, model={}", target.getType(), target.getName(), target.getModel());
        return target;
    }

    public synchronized void delete(String type) {
        if (type == null) return;
        String t = type.trim().toLowerCase();
        ModelRegistry root = readFileSafe();
        root.getModels().removeIf(m -> m.getType().equalsIgnoreCase(t));
        writeFile(root);
        registry.remove(t);
        eventPublisher.publishEvent(new ModelChangedEvent(t));
        log.info("模型已删除: type={}", t);
    }

    /** 解析 apiKey：${ENV} 环境变量占位 / enc: 密文解密 / 明文直返 */
    public String resolveApiKey(ModelConfig cfg) {
        if (cfg == null || cfg.getApiKey() == null) return "";
        return keyCipher.resolve(cfg.getApiKey());
    }

    private Path resolvePath() throws IOException {
        Path p = Paths.get(modelsFilePath);
        if (!p.isAbsolute()) {
            p = Paths.get(System.getProperty("user.dir"), modelsFilePath);
        }
        Path abs = p.toAbsolutePath().normalize();
        if (abs.getParent() != null) Files.createDirectories(abs.getParent());
        return abs;
    }

    private void seedFromExample(Path path) throws IOException {
        InputStream is = new ClassPathResource("models.json.example").getInputStream();
        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        Files.writeString(path, content, StandardCharsets.UTF_8);
        log.info("模型配置文件不存在，已从模板初始化: {}", path.toAbsolutePath());
    }

    private ModelRegistry readFileSafe() {
        try {
            Path path = resolvePath();
            if (!Files.exists(path)) return new ModelRegistry();
            return objectMapper.readValue(Files.readString(path, StandardCharsets.UTF_8), ModelRegistry.class);
        } catch (Exception e) {
            log.error("读取模型配置文件失败，返回空注册表", e);
            return new ModelRegistry();
        }
    }

    private void writeFile(ModelRegistry root) {
        try {
            Path path = resolvePath();
            Files.writeString(path, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("写入模型配置文件失败", e);
            throw new RuntimeException("模型配置保存失败: " + e.getMessage(), e);
        }
    }
}