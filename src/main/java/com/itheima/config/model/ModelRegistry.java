package com.itheima.config.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ModelRegistry {
    private List<ModelConfig> models = new ArrayList<>();
}