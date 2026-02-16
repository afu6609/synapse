package com.synapse.embedding.service;

import com.synapse.embedding.config.EmbeddingConfig;
import com.synapse.embedding.model.EmbeddingResult;
import com.synapse.embedding.model.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15.BgeSmallZhV15EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final EmbeddingConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private volatile EmbeddingModel localModel;
    private volatile String localModelName;

    public EmbeddingService(EmbeddingConfig config, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    private record Chunk(
        int index,
        int originalMessageIndex,
        String id,
        String content,
        String role
    ) {}

    public List<EmbeddingResult> embedWithSlidingWindow(String chatId, List<Message> messages, int windowSize) {
        List<Chunk> chunks = chunkMessages(messages);
        List<EmbeddingResult> results = new ArrayList<>();
        String separator = config.getSlidingWindow().getSeparator();

        for (int i = 0; i <= chunks.size() - windowSize; i++) {
            List<Chunk> window = chunks.subList(i, i + windowSize);

            String combinedContent = window.stream()
                    .map(Chunk::content)
                    .collect(Collectors.joining(separator));

            List<String> chunkIds = window.stream()
                    .map(Chunk::id)
                    .toList();

            String windowId = String.join("_", chunkIds);

            List<Float> vector = getEmbedding(combinedContent);

            results.add(new EmbeddingResult(windowId, combinedContent, vector, chunkIds, i));
            log.debug("Embedded window: {} with {} dimensions, index: {}", windowId, vector.size(), i);
        }

        return results;
    }

    public List<EmbeddingResult> embedIndividually(String chatId, List<Message> messages) {
        List<Chunk> chunks = chunkMessages(messages);
        List<EmbeddingResult> results = new ArrayList<>();

        for (Chunk chunk : chunks) {
            List<Float> vector = getEmbedding(chunk.content());

            results.add(new EmbeddingResult(chunk.id(), chunk.content(), vector, List.of(chunk.id()), chunk.index()));
            log.debug("Embedded chunk: {} with {} dimensions, index: {}", chunk.id(), vector.size(), chunk.index());
        }

        return results;
    }

    List<Chunk> chunkMessages(List<Message> messages) {
        var chunkConfig = config.getChunk();
        int maxLength = chunkConfig.getMaxLength();
        boolean chunkEnabled = chunkConfig.isEnabled();

        List<Chunk> chunks = new ArrayList<>();
        int chunkIndex = 0;
        String contextAnchor = null;

        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);

            if ("user".equals(msg.role())) {
                contextAnchor = msg.content();
            }

            if (!chunkEnabled || msg.content().length() <= maxLength) {
                String content = "[" + msg.role() + "]: " + msg.content();
                chunks.add(new Chunk(chunkIndex++, i, msg.id(), content, msg.role()));
            } else {
                List<String> parts = splitIntoChunks(msg.content(), maxLength);
                for (int j = 0; j < parts.size(); j++) {
                    String partContent;
                    if ("assistant".equals(msg.role()) && contextAnchor != null) {
                        String anchor = contextAnchor.length() > 200
                                ? contextAnchor.substring(0, 200)
                                : contextAnchor;
                        partContent = "[Question]: " + anchor + "\n---\n[assistant]: " + parts.get(j);
                    } else {
                        partContent = "[" + msg.role() + "]: " + parts.get(j);
                    }
                    String chunkId = msg.id() + "_chunk" + j;
                    chunks.add(new Chunk(chunkIndex++, i, chunkId, partContent, msg.role()));
                }
            }
        }

        log.debug("Chunked {} messages into {} chunks", messages.size(), chunks.size());
        return chunks;
    }

    private List<String> splitIntoChunks(String text, int maxLength) {
        // 1. Split by double newlines (paragraphs)
        String[] paragraphs = text.split("\n\n");
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) continue;

            if (paragraph.length() > maxLength) {
                // Flush current buffer first
                if (!current.isEmpty()) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                // 3. Try splitting long paragraph by single newlines
                splitLongParagraph(paragraph, maxLength, result);
            } else if (current.isEmpty()) {
                current.append(paragraph);
            } else if (current.length() + 2 + paragraph.length() <= maxLength) {
                // 2. Merge small paragraphs
                current.append("\n\n").append(paragraph);
            } else {
                result.add(current.toString());
                current.setLength(0);
                current.append(paragraph);
            }
        }

        if (!current.isEmpty()) {
            result.add(current.toString());
        }

        return result;
    }

    private void splitLongParagraph(String paragraph, int maxLength, List<String> result) {
        // Try splitting by single newlines
        String[] lines = paragraph.split("\n");
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            if (line.length() > maxLength) {
                // Flush current buffer
                if (!current.isEmpty()) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                // 4. Hard split by maxLength
                hardSplit(line, maxLength, result);
            } else if (current.isEmpty()) {
                current.append(line);
            } else if (current.length() + 1 + line.length() <= maxLength) {
                current.append("\n").append(line);
            } else {
                result.add(current.toString());
                current.setLength(0);
                current.append(line);
            }
        }

        if (!current.isEmpty()) {
            result.add(current.toString());
        }
    }

    private void hardSplit(String text, int maxLength, List<String> result) {
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxLength, text.length());
            result.add(text.substring(start, end));
            start = end;
        }
    }

    public List<Float> getEmbedding(String text) {
        var provider = config.getProvider();
        if (!provider.isConfigured()) {
            throw new IllegalStateException("Embedding provider is not configured");
        }

        if ("local".equals(provider.getType())) {
            return getLocalEmbedding(text, provider.getModel());
        }

        Map<String, Object> requestBody = Map.of(
                "model", provider.getModel(),
                "input", text,
                "encoding_format", "float"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (provider.getApiKey() != null && !provider.getApiKey().isBlank()) {
            headers.set("Authorization", "Bearer " + provider.getApiKey());
        }

        String response = restTemplate.postForObject(
                provider.getBaseUrl() + "/embeddings",
                new HttpEntity<>(requestBody, headers),
                String.class);

        return parseEmbeddingResponse(response);
    }

    private List<Float> getLocalEmbedding(String text, String modelName) {
        EmbeddingModel model = getOrCreateLocalModel(modelName);
        float[] vector = model.embed(text).content().vector();
        List<Float> result = new ArrayList<>(vector.length);
        for (float v : vector) {
            result.add(v);
        }
        return result;
    }

    private EmbeddingModel getOrCreateLocalModel(String modelName) {
        if (localModel != null && modelName.equals(localModelName)) {
            return localModel;
        }
        synchronized (this) {
            if (localModel != null && modelName.equals(localModelName)) {
                return localModel;
            }
            localModel = createLocalModel(modelName);
            localModelName = modelName;
            return localModel;
        }
    }

    private EmbeddingModel createLocalModel(String modelName) {
        log.info("Loading local embedding model: {}", modelName);
        return switch (modelName) {
            case "bge-small-zh-v15" -> new BgeSmallZhV15EmbeddingModel();
            default -> throw new IllegalArgumentException("Unknown local model: " + modelName);
        };
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
}
