package com.chatst.embeddingdemo.service;

import com.chatst.embeddingdemo.config.EmbeddingConfig;
import com.chatst.embeddingdemo.model.SearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
public class RerankService {

    private static final Logger log = LoggerFactory.getLogger(RerankService.class);

    private final EmbeddingConfig config;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public RerankService(EmbeddingConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder().build();
    }

    /**
     * 对搜索结果进行重排序
     */
    public List<SearchResult> rerank(String query, List<SearchResult> candidates, int topK) {
        if (candidates.isEmpty()) {
            return candidates;
        }

        var siliconConfig = config.getSiliconflow();

        List<String> documents = candidates.stream()
                .map(SearchResult::content)
                .toList();

        Map<String, Object> requestBody = Map.of(
                "model", siliconConfig.getRerankModel(),
                "query", query,
                "documents", documents,
                "top_n", Math.min(topK, candidates.size()),
                "return_documents", false
        );

        try {
            String response = webClient.post()
                    .uri(siliconConfig.getBaseUrl() + "/rerank")
                    .header("Authorization", "Bearer " + siliconConfig.getApiKey())
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseRerankResponse(response, candidates);
        } catch (Exception e) {
            log.error("Rerank failed, returning original order", e);
            return candidates.subList(0, Math.min(topK, candidates.size()));
        }
    }

    private List<SearchResult> parseRerankResponse(String response, List<SearchResult> candidates) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.path("results");

            List<SearchResult> reranked = new ArrayList<>();
            for (JsonNode result : results) {
                int index = result.path("index").asInt();
                double score = result.path("relevance_score").asDouble();

                SearchResult original = candidates.get(index);
                reranked.add(new SearchResult(
                        original.windowId(),
                        original.content(),
                        score,
                        original.messageIds()
                ));
            }

            return reranked;
        } catch (Exception e) {
            log.error("Failed to parse rerank response: {}", response, e);
            throw new RuntimeException("Failed to parse rerank response", e);
        }
    }
}
