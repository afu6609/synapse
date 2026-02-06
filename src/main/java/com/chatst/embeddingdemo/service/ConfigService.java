package com.chatst.embeddingdemo.service;

import com.chatst.embeddingdemo.config.EmbeddingConfig;
import com.chatst.embeddingdemo.websocket.EmbeddingWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.*;

@Service
public class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

    private final EmbeddingConfig config;
    private final EmbeddingService embeddingService;
    private final VectorStorageService vectorStorageService;
    private final EmbeddingWebSocketHandler webSocketHandler;

    public ConfigService(EmbeddingConfig config,
                         EmbeddingService embeddingService,
                         VectorStorageService vectorStorageService,
                         @Lazy EmbeddingWebSocketHandler webSocketHandler) {
        this.config = config;
        this.embeddingService = embeddingService;
        this.vectorStorageService = vectorStorageService;
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * 返回当前配置快照（API Key 脱敏）
     */
    public Map<String, Object> getConfigSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("provider", config.getProvider());

        // SiliconFlow
        snapshot.put("siliconflow.baseUrl", config.getSiliconflow().getBaseUrl());
        snapshot.put("siliconflow.apiKey", maskApiKey(config.getSiliconflow().getApiKey()));
        snapshot.put("siliconflow.embeddingModel", config.getSiliconflow().getEmbeddingModel());
        snapshot.put("siliconflow.rerankModel", config.getSiliconflow().getRerankModel());

        // Ollama
        snapshot.put("ollama.baseUrl", config.getOllama().getBaseUrl());
        snapshot.put("ollama.embeddingModel", config.getOllama().getEmbeddingModel());

        // Sliding Window
        snapshot.put("slidingWindow.size", config.getSlidingWindow().getSize());
        snapshot.put("slidingWindow.separator", config.getSlidingWindow().getSeparator());

        // Storage
        snapshot.put("storage.basePath", config.getStorage().getBasePath());
        snapshot.put("storage.vectorFileSuffix", config.getStorage().getVectorFileSuffix());

        // Detected dimension
        snapshot.put("detectedDimension", config.getDetectedDimension());

        return snapshot;
    }

    /**
     * 部分更新配置，返回变更的字段列表
     */
    public List<String> applyConfigUpdate(Map<String, Object> updates) {
        List<String> changedFields = new ArrayList<>();
        boolean modelOrUrlChanged = false;
        boolean storagePathChanged = false;

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            boolean changed = applyField(key, value);
            if (changed) {
                changedFields.add(key);
                if (isModelOrUrlField(key)) {
                    modelOrUrlChanged = true;
                }
                if ("storage.basePath".equals(key)) {
                    storagePathChanged = true;
                }
            }
        }

        if (changedFields.isEmpty()) {
            return changedFields;
        }

        // 模型/URL 变更 → 自动检测维度
        if (modelOrUrlChanged) {
            try {
                detectDimension();
            } catch (Exception e) {
                log.warn("Auto dimension detection failed after config change: {}", e.getMessage());
            }
        }

        // 存储路径变更 → 刷新 VectorStorageService
        if (storagePathChanged) {
            try {
                refreshStoragePath();
            } catch (Exception e) {
                log.warn("Failed to refresh storage path: {}", e.getMessage());
            }
        }

        // 广播配置变更
        broadcastConfigChange(changedFields);

        log.info("Config updated, changed fields: {}", changedFields);
        return changedFields;
    }

    /**
     * 手动触发维度检测
     */
    public Integer detectDimension() {
        try {
            List<Float> testVector = embeddingService.getEmbedding("test");
            int dimension = testVector.size();
            config.setDetectedDimension(dimension);
            log.info("Detected embedding dimension: {}", dimension);
            return dimension;
        } catch (Exception e) {
            log.error("Dimension detection failed", e);
            throw new RuntimeException("Dimension detection failed: " + e.getMessage(), e);
        }
    }

    private boolean applyField(String key, Object value) {
        String strVal = value != null ? value.toString() : null;
        return switch (key) {
            case "provider" -> {
                if (!Objects.equals(config.getProvider(), strVal)) {
                    config.setProvider(strVal);
                    yield true;
                }
                yield false;
            }
            case "siliconflow.baseUrl" -> {
                if (!Objects.equals(config.getSiliconflow().getBaseUrl(), strVal)) {
                    config.getSiliconflow().setBaseUrl(strVal);
                    yield true;
                }
                yield false;
            }
            case "siliconflow.apiKey" -> {
                if (!Objects.equals(config.getSiliconflow().getApiKey(), strVal)) {
                    config.getSiliconflow().setApiKey(strVal);
                    yield true;
                }
                yield false;
            }
            case "siliconflow.embeddingModel" -> {
                if (!Objects.equals(config.getSiliconflow().getEmbeddingModel(), strVal)) {
                    config.getSiliconflow().setEmbeddingModel(strVal);
                    yield true;
                }
                yield false;
            }
            case "siliconflow.rerankModel" -> {
                if (!Objects.equals(config.getSiliconflow().getRerankModel(), strVal)) {
                    config.getSiliconflow().setRerankModel(strVal);
                    yield true;
                }
                yield false;
            }
            case "ollama.baseUrl" -> {
                if (!Objects.equals(config.getOllama().getBaseUrl(), strVal)) {
                    config.getOllama().setBaseUrl(strVal);
                    yield true;
                }
                yield false;
            }
            case "ollama.embeddingModel" -> {
                if (!Objects.equals(config.getOllama().getEmbeddingModel(), strVal)) {
                    config.getOllama().setEmbeddingModel(strVal);
                    yield true;
                }
                yield false;
            }
            case "slidingWindow.size" -> {
                int intVal = value instanceof Number n ? n.intValue() : Integer.parseInt(strVal);
                if (config.getSlidingWindow().getSize() != intVal) {
                    config.getSlidingWindow().setSize(intVal);
                    yield true;
                }
                yield false;
            }
            case "slidingWindow.separator" -> {
                if (!Objects.equals(config.getSlidingWindow().getSeparator(), strVal)) {
                    config.getSlidingWindow().setSeparator(strVal);
                    yield true;
                }
                yield false;
            }
            case "storage.basePath" -> {
                if (!Objects.equals(config.getStorage().getBasePath(), strVal)) {
                    config.getStorage().setBasePath(strVal);
                    yield true;
                }
                yield false;
            }
            case "storage.vectorFileSuffix" -> {
                if (!Objects.equals(config.getStorage().getVectorFileSuffix(), strVal)) {
                    config.getStorage().setVectorFileSuffix(strVal);
                    yield true;
                }
                yield false;
            }
            default -> {
                log.warn("Unknown config key: {}", key);
                yield false;
            }
        };
    }

    private boolean isModelOrUrlField(String key) {
        return key.equals("provider")
                || key.equals("siliconflow.baseUrl")
                || key.equals("siliconflow.embeddingModel")
                || key.equals("ollama.baseUrl")
                || key.equals("ollama.embeddingModel");
    }

    private void refreshStoragePath() throws Exception {
        String basePath = config.getStorage().getBasePath();
        vectorStorageService.updateBasePath(Path.of(basePath));
    }

    private void broadcastConfigChange(List<String> changedFields) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "config_changed");
        message.put("changedFields", changedFields);
        message.put("config", getConfigSnapshot());
        webSocketHandler.broadcast(message);
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
