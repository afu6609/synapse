package com.chatst.embeddingdemo.service;

import com.chatst.embeddingdemo.config.EmbeddingConfig;
import com.chatst.embeddingdemo.model.EmbeddingResult;
import com.chatst.embeddingdemo.model.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final EmbeddingConfig config;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public EmbeddingService(EmbeddingConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder().build();
    }

    /**
     * 创建滑动窗口并向量化
     */
    public List<EmbeddingResult> embedWithSlidingWindow(String chatId, List<Message> messages) {
        List<EmbeddingResult> results = new ArrayList<>();
        int windowSize = config.getSlidingWindow().getSize();
        String separator = config.getSlidingWindow().getSeparator();

        for (int i = 0; i <= messages.size() - windowSize; i++) {
            List<Message> window = messages.subList(i, i + windowSize);

            // 组合窗口内的消息
            String combinedContent = window.stream()
                    .map(m -> "[" + m.role() + "]: " + m.content())
                    .collect(Collectors.joining(separator));

            List<String> messageIds = window.stream()
                    .map(Message::id)
                    .toList();

            String windowId = String.join("_", messageIds);

            // 调用embedding API
            List<Float> vector = getEmbedding(combinedContent);

            results.add(new EmbeddingResult(windowId, combinedContent, vector, messageIds));
            log.debug("Embedded window: {} with {} dimensions", windowId, vector.size());
        }

        return results;
    }

    /**
     * 单条文本向量化
     */
    public List<Float> getEmbedding(String text) {
        return switch (config.getProvider()) {
            case "ollama" -> getOllamaEmbedding(text);
            default -> getSiliconFlowEmbedding(text);
        };
    }

    private List<Float> getSiliconFlowEmbedding(String text) {
        var siliconConfig = config.getSiliconflow();

        Map<String, Object> requestBody = Map.of(
                "model", siliconConfig.getEmbeddingModel(),
                "input", text,
                "encoding_format", "float"
        );

        String response = webClient.post()
                .uri(siliconConfig.getBaseUrl() + "/embeddings")
                .header("Authorization", "Bearer " + siliconConfig.getApiKey())
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseEmbeddingResponse(response);
    }

    private List<Float> getOllamaEmbedding(String text) {
        var ollamaConfig = config.getOllama();

        Map<String, Object> requestBody = Map.of(
                "model", ollamaConfig.getEmbeddingModel(),
                "input", text
        );

        String response = webClient.post()
                .uri(ollamaConfig.getBaseUrl() + "/api/embed")
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseOllamaEmbeddingResponse(response);
    }

    private List<Float> parseEmbeddingResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode embedding = root.path("data").get(0).path("embedding");
            List<Float> result = new ArrayList<>();
            for (JsonNode node : embedding) {
                result.add(node.floatValue());
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to parse embedding response: {}", response, e);
            throw new RuntimeException("Failed to parse embedding response", e);
        }
    }

    private List<Float> parseOllamaEmbeddingResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode embeddings = root.path("embeddings").get(0);
            List<Float> result = new ArrayList<>();
            for (JsonNode node : embeddings) {
                result.add(node.floatValue());
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to parse Ollama embedding response: {}", response, e);
            throw new RuntimeException("Failed to parse Ollama embedding response", e);
        }
    }
}
