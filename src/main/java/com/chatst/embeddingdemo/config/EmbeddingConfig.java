package com.chatst.embeddingdemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingConfig {

    private SiliconFlowConfig siliconflow = new SiliconFlowConfig();
    private OllamaConfig ollama = new OllamaConfig();
    private String provider = "siliconflow";
    private SlidingWindowConfig slidingWindow = new SlidingWindowConfig();
    private StorageConfig storage = new StorageConfig();

    public static class SiliconFlowConfig {
        private String baseUrl = "https://api.siliconflow.cn/v1";
        private String apiKey;
        private String embeddingModel = "Qwen/Qwen3-Embedding-8B";
        private String rerankModel = "Qwen/Qwen3-Reranker-8B";

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getEmbeddingModel() { return embeddingModel; }
        public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
        public String getRerankModel() { return rerankModel; }
        public void setRerankModel(String rerankModel) { this.rerankModel = rerankModel; }
    }

    public static class OllamaConfig {
        private String baseUrl = "http://localhost:11434";
        private String embeddingModel = "qwen3-embedding-4b";

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getEmbeddingModel() { return embeddingModel; }
        public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
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

    public SiliconFlowConfig getSiliconflow() { return siliconflow; }
    public void setSiliconflow(SiliconFlowConfig siliconflow) { this.siliconflow = siliconflow; }
    public OllamaConfig getOllama() { return ollama; }
    public void setOllama(OllamaConfig ollama) { this.ollama = ollama; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public SlidingWindowConfig getSlidingWindow() { return slidingWindow; }
    public void setSlidingWindow(SlidingWindowConfig slidingWindow) { this.slidingWindow = slidingWindow; }
    public StorageConfig getStorage() { return storage; }
    public void setStorage(StorageConfig storage) { this.storage = storage; }
}
