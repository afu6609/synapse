package com.synapse.embedding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingConfig {

    private ProviderConfig provider = new ProviderConfig();
    private ProviderConfig rerank = new ProviderConfig();
    private SlidingWindowConfig slidingWindow = new SlidingWindowConfig();
    private StorageConfig storage = new StorageConfig();
    private ChunkConfig chunk = new ChunkConfig();
    private GraphConfig graph = new GraphConfig();
    private Integer detectedDimension;

    /**
     *统一的提供商配置-用于嵌入和重新排名。
     *所有供应商使用openai兼容的API格式。
     *对于Ollama，设置baseUrl为http://localhost:11434/v1。
     */
    public static class ProviderConfig {
        private String type = "api"; // "api"（默认，外部HTTP）或 "local"（本地ONNX模型）
        private String baseUrl;
        private String model;
        private String apiKey;

        public boolean isConfigured() {
            if ("local".equals(type)) {
                return model != null && !model.isBlank();
            }
            return baseUrl != null && !baseUrl.isBlank()
                    && model != null && !model.isBlank();
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    public static class SlidingWindowConfig {
        private int size = 2;
        private String separator = "\n---\n";

        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public String getSeparator() { return separator; }
        public void setSeparator(String separator) { this.separator = separator; }
    }

    public static class StorageConfig {
        private String basePath;
        private String vectorFileSuffix = ".vec";

        public String getBasePath() { return basePath; }
        public void setBasePath(String basePath) { this.basePath = basePath; }
        public String getVectorFileSuffix() { return vectorFileSuffix; }
        public void setVectorFileSuffix(String vectorFileSuffix) { this.vectorFileSuffix = vectorFileSuffix; }
    }

    public static class ChunkConfig {
        private boolean enabled = true;
        private int maxLength = 512;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getMaxLength() { return maxLength; }
        public void setMaxLength(int maxLength) { this.maxLength = maxLength; }
    }

    public static class GraphConfig {
        private boolean enabled = true;
        private double decayFactor = 0.95;
        private double pruneThreshold = 0.01;
        private double queryThreshold = 0.5;
        private int maxGraphResults = 5;
        private String decayCron = "0 0 3 * * *";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public double getDecayFactor() { return decayFactor; }
        public void setDecayFactor(double decayFactor) { this.decayFactor = decayFactor; }
        public double getPruneThreshold() { return pruneThreshold; }
        public void setPruneThreshold(double pruneThreshold) { this.pruneThreshold = pruneThreshold; }
        public double getQueryThreshold() { return queryThreshold; }
        public void setQueryThreshold(double queryThreshold) { this.queryThreshold = queryThreshold; }
        public int getMaxGraphResults() { return maxGraphResults; }
        public void setMaxGraphResults(int maxGraphResults) { this.maxGraphResults = maxGraphResults; }
        public String getDecayCron() { return decayCron; }
        public void setDecayCron(String decayCron) { this.decayCron = decayCron; }
    }

    public ProviderConfig getProvider() { return provider; }
    public void setProvider(ProviderConfig provider) { this.provider = provider; }
    public ProviderConfig getRerank() { return rerank; }
    public void setRerank(ProviderConfig rerank) { this.rerank = rerank; }
    public SlidingWindowConfig getSlidingWindow() { return slidingWindow; }
    public void setSlidingWindow(SlidingWindowConfig slidingWindow) { this.slidingWindow = slidingWindow; }
    public StorageConfig getStorage() { return storage; }
    public void setStorage(StorageConfig storage) { this.storage = storage; }
    public ChunkConfig getChunk() { return chunk; }
    public void setChunk(ChunkConfig chunk) { this.chunk = chunk; }
    public GraphConfig getGraph() { return graph; }
    public void setGraph(GraphConfig graph) { this.graph = graph; }
    public Integer getDetectedDimension() { return detectedDimension; }
    public void setDetectedDimension(Integer detectedDimension) { this.detectedDimension = detectedDimension; }
}
