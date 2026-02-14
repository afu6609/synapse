package com.chatst.embeddingdemo.service;

import com.chatst.embeddingdemo.config.EmbeddingConfig;
import com.chatst.embeddingdemo.event.ConfigChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.file.Path;
import java.util.*;

@Service
public class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

    private final EmbeddingConfig config;
    private final EmbeddingService embeddingService;
    private final VectorStorageService vectorStorageService;
    private final ApplicationEventPublisher eventPublisher;

    public ConfigService(EmbeddingConfig config,
                         EmbeddingService embeddingService,
                         VectorStorageService vectorStorageService,
                         ApplicationEventPublisher eventPublisher) {
        this.config = config;
        this.embeddingService = embeddingService;
        this.vectorStorageService = vectorStorageService;
        this.eventPublisher = eventPublisher;
    }

    public Map<String, Object> getConfigSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        // Embedding provider
        snapshot.put("provider.type", config.getProvider().getType());
        snapshot.put("provider.baseUrl", config.getProvider().getBaseUrl());
        snapshot.put("provider.model", config.getProvider().getModel());
        snapshot.put("provider.apiKey", maskApiKey(config.getProvider().getApiKey()));

        // Rerank provider
        snapshot.put("rerank.baseUrl", config.getRerank().getBaseUrl());
        snapshot.put("rerank.model", config.getRerank().getModel());
        snapshot.put("rerank.apiKey", maskApiKey(config.getRerank().getApiKey()));

        // Sliding Window
        snapshot.put("slidingWindow.size", Integer.valueOf(config.getSlidingWindow().getSize()));
        snapshot.put("slidingWindow.separator", config.getSlidingWindow().getSeparator());

        // Storage
        snapshot.put("storage.basePath", config.getStorage().getBasePath());
        snapshot.put("storage.vectorFileSuffix", config.getStorage().getVectorFileSuffix());

        // Chunk
        snapshot.put("chunk.enabled", Boolean.valueOf(config.getChunk().isEnabled()));
        snapshot.put("chunk.maxLength", Integer.valueOf(config.getChunk().getMaxLength()));

        // Detected dimension
        snapshot.put("detectedDimension", config.getDetectedDimension());

        return snapshot;
    }

    public synchronized List<String> applyConfigUpdate(Map<String, Object> updates) {
        List<String> changedFields = new ArrayList<>();
        boolean providerChanged = false;
        boolean storagePathChanged = false;

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            boolean changed = applyField(key, value);
            if (changed) {
                changedFields.add(key);
                if (isProviderField(key)) {
                    providerChanged = true;
                }
                if ("storage.basePath".equals(key)) {
                    storagePathChanged = true;
                }
            }
        }

        if (changedFields.isEmpty()) {
            return changedFields;
        }

        // Provider changes → auto-detect dimension
        if (providerChanged && config.getProvider().isConfigured()) {
            try {
                detectDimension();
            } catch (Exception e) {
                log.warn("Auto dimension detection failed after config change: {}", e.getMessage());
            }
        }

        // Storage path changed → refresh VectorStorageService
        if (storagePathChanged) {
            try {
                refreshStoragePath();
            } catch (Exception e) {
                log.warn("Failed to refresh storage path: {}", e.getMessage());
            }
        }

        broadcastConfigChange(changedFields);

        log.info("Config updated, changed fields: {}", changedFields);
        return changedFields;
    }

    public Integer detectDimension() {
        try {
            List<Float> testVector = embeddingService.getEmbedding("test");
            Integer dimension = Integer.valueOf(testVector.size());
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
            // Embedding provider
            case "provider.type" -> {
                if (!Objects.equals(config.getProvider().getType(), strVal)) {
                    config.getProvider().setType(strVal);
                    yield true;
                }
                yield false;
            }
            case "provider.baseUrl" -> {
                if (!Objects.equals(config.getProvider().getBaseUrl(), strVal)) {
                    config.getProvider().setBaseUrl(strVal);
                    yield true;
                }
                yield false;
            }
            case "provider.model" -> {
                if (!Objects.equals(config.getProvider().getModel(), strVal)) {
                    config.getProvider().setModel(strVal);
                    yield true;
                }
                yield false;
            }
            case "provider.apiKey" -> {
                if (!Objects.equals(config.getProvider().getApiKey(), strVal)) {
                    config.getProvider().setApiKey(strVal);
                    yield true;
                }
                yield false;
            }
            // Rerank provider
            case "rerank.baseUrl" -> {
                if (!Objects.equals(config.getRerank().getBaseUrl(), strVal)) {
                    config.getRerank().setBaseUrl(strVal);
                    yield true;
                }
                yield false;
            }
            case "rerank.model" -> {
                if (!Objects.equals(config.getRerank().getModel(), strVal)) {
                    config.getRerank().setModel(strVal);
                    yield true;
                }
                yield false;
            }
            case "rerank.apiKey" -> {
                if (!Objects.equals(config.getRerank().getApiKey(), strVal)) {
                    config.getRerank().setApiKey(strVal);
                    yield true;
                }
                yield false;
            }
            // Sliding window
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
            // Storage
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
            // Chunk
            case "chunk.enabled" -> {
                boolean boolVal = value instanceof Boolean b ? b : Boolean.parseBoolean(strVal);
                if (config.getChunk().isEnabled() != boolVal) {
                    config.getChunk().setEnabled(boolVal);
                    yield true;
                }
                yield false;
            }
            case "chunk.maxLength" -> {
                int intVal = value instanceof Number n ? n.intValue() : Integer.parseInt(strVal);
                if (config.getChunk().getMaxLength() != intVal) {
                    config.getChunk().setMaxLength(intVal);
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

    private boolean isProviderField(String key) {
        return key.equals("provider.type")
                || key.equals("provider.baseUrl")
                || key.equals("provider.model");
    }

    private void refreshStoragePath() throws Exception {
        String basePath = config.getStorage().getBasePath();
        vectorStorageService.updateBasePath(Path.of(basePath));
    }

    private void broadcastConfigChange(List<String> changedFields) {
        eventPublisher.publishEvent(new ConfigChangedEvent(this, changedFields, getConfigSnapshot()));
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
