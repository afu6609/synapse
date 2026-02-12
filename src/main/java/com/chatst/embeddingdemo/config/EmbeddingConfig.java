package com.chatst.embeddingdemo.config;

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
    private Integer detectedDimension;

    /**
     * Unified provider config — used for both embedding and reranking.
     * All providers use OpenAI-compatible API format.
     * For Ollama, set baseUrl to http://localhost:11434/v1.
     */
    public static class ProviderConfig {
        private String baseUrl;
        private String model;
        private String apiKey;

        public boolean isConfigured() {
            return baseUrl != null && !baseUrl.isBlank()
                    && model != null && !model.isBlank();
        }

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
    public Integer getDetectedDimension() { return detectedDimension; }
    public void setDetectedDimension(Integer detectedDimension) { this.detectedDimension = detectedDimension; }
}
