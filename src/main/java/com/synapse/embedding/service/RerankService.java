package com.synapse.embedding.service;

import com.synapse.embedding.config.EmbeddingConfig;
import com.synapse.embedding.model.SearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class RerankService {

    private static final Logger log = LoggerFactory.getLogger(RerankService.class);

    private final EmbeddingConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public RerankService(EmbeddingConfig config, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public List<SearchResult> rerank(String query, List<SearchResult> candidates, int topK) {
        if (candidates.isEmpty()) {
            return candidates;
        }

        var rerankConfig = config.getRerank();

        if (!rerankConfig.isConfigured()) {
            log.warn("Rerank provider is not configured, returning original order");
            return candidates.subList(0, Math.min(topK, candidates.size()));
        }

        List<String> documents = candidates.stream()
                .map(SearchResult::content)
                .toList();

        // 同时发送 documents (Cohere/SiliconFlow/Jina) 和 texts (HuggingFace TEI)，
        // API 会各取所需、忽略多余字段
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", rerankConfig.getModel());
        requestBody.put("query", query);
        requestBody.put("documents", documents);
        requestBody.put("texts", documents);
        requestBody.put("top_n", Math.min(topK, candidates.size()));
        requestBody.put("return_documents", false);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (rerankConfig.getApiKey() != null && !rerankConfig.getApiKey().isBlank()) {
                headers.set("Authorization", "Bearer " + rerankConfig.getApiKey());
            }

            String response = restTemplate.postForObject(
                    rerankConfig.getBaseUrl() + "/rerank",
                    new HttpEntity<>(requestBody, headers),
                    String.class);

            return parseRerankResponse(response, candidates, topK);
        } catch (Exception e) {
            log.error("Rerank failed, returning original order", e);
            return candidates.subList(0, Math.min(topK, candidates.size()));
        }
    }

    private List<SearchResult> parseRerankResponse(String response, List<SearchResult> candidates, int topK) {
        try {
            JsonNode root = objectMapper.readTree(response);

            // 兼容两种响应格式：
            // Cohere/SiliconFlow/Jina: {"results": [{"index":0, "relevance_score":0.95}]}
            // HuggingFace TEI:         [{"index":0, "score":0.95}]
            JsonNode results;
            if (root.isArray()) {
                results = root;
            } else {
                results = root.path("results");
            }

            List<SearchResult> reranked = new ArrayList<>();
            for (JsonNode result : results) {
                int index = result.path("index").asInt();
                // TEI 用 "score"，Cohere/SiliconFlow 用 "relevance_score"
                double score = result.has("relevance_score")
                        ? result.path("relevance_score").asDouble()
                        : result.path("score").asDouble();

                if (index < candidates.size()) {
                    SearchResult original = candidates.get(index);
                    reranked.add(new SearchResult(
                            original.windowId(),
                            original.content(),
                            score,
                            original.messageIds(),
                            original.messageIndex(),
                            original.matchType()
                    ));
                }
            }

            // TEI 返回全部结果不做 top_n 截断，这里统一截断
            if (reranked.size() > topK) {
                reranked = reranked.subList(0, topK);
            }

            return reranked;
        } catch (Exception e) {
            log.error("Failed to parse rerank response: {}", response, e);
            throw new RuntimeException("Failed to parse rerank response", e);
        }
    }
}
