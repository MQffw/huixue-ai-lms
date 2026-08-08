package com.itheima.controller;

import com.itheima.config.model.ModelConfig;
import com.itheima.config.model.ModelKeyCipher;
import com.itheima.config.model.ModelRegistryService;
import com.itheima.pojo.Result;
import com.itheima.security.annotation.RequiresRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型注册表管理接口
 * - GET 列表：前端模型下拉框数据源（apiKey 脱敏返回）
 * - POST 新增/修改：保存后立即生效（无需重启）
 * - DELETE 删除
 */
@Tag(name = "模型注册表", description = "动态管理 AI 模型配置，新增模型无需改代码")
@RestController
@RequestMapping("/ai/models")
@RequiredArgsConstructor
public class AiModelController {

    private final ModelRegistryService modelRegistryService;
    private final ModelKeyCipher keyCipher;

    @Operation(summary = "模型列表", description = "返回当前启用的模型配置（apiKey 已脱敏）")
    @GetMapping
    public Result list() {
        List<ModelConfig> masked = new ArrayList<>();
        for (ModelConfig cfg : modelRegistryService.list()) {
            masked.add(maskedCopy(cfg));
        }
        return Result.success(masked);
    }

    @Operation(summary = "新增/修改模型", description = "type 已存在则更新，不存在则新增；保存后立即生效")
    @PostMapping
    @RequiresRole("ADMIN")
    public Result save(@RequestBody ModelConfig config) {
        return Result.success(maskedCopy(modelRegistryService.save(config)));
    }

    @Operation(summary = "删除模型", description = "删除后该模型立即不可用")
    @DeleteMapping("/{type}")
    @RequiresRole("ADMIN")
    public Result delete(@PathVariable String type) {
        modelRegistryService.delete(type);
        return Result.success();
    }

    private ModelConfig maskedCopy(ModelConfig cfg) {
        ModelConfig copy = new ModelConfig();
        copy.setType(cfg.getType());
        copy.setName(cfg.getName());
        copy.setBaseUrl(cfg.getBaseUrl());
        copy.setModel(cfg.getModel());
        copy.setTemperature(cfg.getTemperature());
        copy.setMaxTokens(cfg.getMaxTokens());
        copy.setEnabled(cfg.getEnabled());
        copy.setApiKey(keyCipher.mask(cfg.getApiKey()));
        return copy;
    }
}