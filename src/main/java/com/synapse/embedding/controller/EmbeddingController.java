package com.synapse.embedding.controller;

import com.synapse.embedding.config.EmbeddingConfig;
import com.synapse.embedding.model.*;
import com.synapse.embedding.service.EmbeddingService;
import com.synapse.embedding.service.MemoryGraphService;
import com.synapse.embedding.service.RerankService;
import com.synapse.embedding.service.VectorStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class EmbeddingController {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingController.class);

    private final EmbeddingService embeddingService;
    private final RerankService rerankService;
    private final VectorStorageService storageService;
    private final MemoryGraphService memoryGraphService;
    private final EmbeddingConfig config;

    public EmbeddingController(EmbeddingService embeddingService,
                               RerankService rerankService,
                               VectorStorageService storageService,
                               MemoryGraphService memoryGraphService,
                               EmbeddingConfig config) {
        this.embeddingService = embeddingService;
        this.rerankService = rerankService;
        this.storageService = storageService;
        this.memoryGraphService = memoryGraphService;
        this.config = config;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "embedding-service"));
    }

    @PostMapping("/embed")
    public ResponseEntity<?> embed(@RequestBody EmbeddingRequest request) {
        if (!config.getProvider().isConfigured()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Embedding provider is not configured"));
        }

        log.info("Embedding request for chat: {}, messages: {}, slidingWindow: {}, windowSize: {}",
                request.chatId(), request.messages().size(), request.useSlidingWindow(), request.windowSize());

        try {
            List<EmbeddingResult> results;

            if (request.useSlidingWindow()) {
                results = embeddingService.embedWithSlidingWindow(
                        request.chatId(),
                        request.messages(),
                        request.windowSize()
                );
            } else {
                results = embeddingService.embedIndividually(
                        request.chatId(),
                        request.messages()
                );
            }

            storageService.saveEmbeddings(request.chatId(), results);

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Embedding failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/embed/text")
    public ResponseEntity<Map<String, Object>> embedText(@RequestBody Map<String, String> request) {
        if (!config.getProvider().isConfigured()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Embedding provider is not configured"));
        }

        String text = request.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "text is required"));
        }

        List<Float> vector = embeddingService.getEmbedding(text);
        return ResponseEntity.ok(Map.of(
                "text", text,
                "vector", vector,
                "dimensions", vector.size()
        ));
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody SearchRequest request) {
        if (!config.getProvider().isConfigured()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Embedding provider is not configured"));
        }

        log.info("Search request for chat: {}, query: {}, nearbyCount: {}, useGraph: {}",
                request.chatId(), request.query(), request.nearbyCount(), request.useGraph());

        try {
            List<SearchResult> results;

            if (request.useRerank()) {
                // search → rerank → addNearby
                results = storageService.search(request.chatId(), request.query(), request.topK() * 2);

                if (!results.isEmpty()) {
                    if (!config.getRerank().isConfigured()) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Rerank provider is not configured"));
                    }
                    results = rerankService.rerank(request.query(), results, request.topK());
                }

                if (request.nearbyCount() > 0) {
                    results = storageService.addNearby(request.chatId(), results, request.nearbyCount());
                }
            } else if (request.nearbyCount() > 0) {
                results = storageService.searchWithNearby(request.chatId(), request.query(), request.topK(), request.nearbyCount());
            } else {
                results = storageService.search(request.chatId(), request.query(), request.topK());
            }

            // 图关联逻辑
            results = applyGraphLogic(request.chatId(), results, request.useGraph());

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Search failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private List<SearchResult> applyGraphLogic(String chatId, List<SearchResult> results, boolean useGraph) {
        var graphConfig = config.getGraph();
        if (!graphConfig.isEnabled() || results.isEmpty()) {
            return results;
        }

        // 收集 vector 直接命中的 windowId 用于共激活记录
        List<String> directHitIds = results.stream()
                .filter(r -> "vector".equals(r.matchType()))
                .map(SearchResult::windowId)
                .toList();

        // 被动学习：始终记录共激活（只要 graph.enabled）
        if (directHitIds.size() >= 2) {
            try {
                memoryGraphService.recordCoActivation(chatId, directHitIds);
            } catch (Exception e) {
                log.warn("Failed to record co-activation", e);
            }
        }

        // useGraph 控制是否在响应中包含图关联结果
        if (useGraph) {
            try {
                Set<String> existingIds = results.stream()
                        .map(SearchResult::windowId)
                        .collect(Collectors.toSet());

                var associations = memoryGraphService.queryAssociations(
                        chatId, new LinkedHashSet<>(directHitIds),
                        graphConfig.getQueryThreshold(), graphConfig.getMaxGraphResults());

                // 排除已存在的 windowId
                Map<String, Double> graphScores = new LinkedHashMap<>();
                for (var assoc : associations) {
                    if (!existingIds.contains(assoc.windowId())) {
                        graphScores.put(assoc.windowId(), assoc.weight());
                    }
                }

                if (!graphScores.isEmpty()) {
                    List<SearchResult> graphResults = storageService.fetchByWindowIds(chatId, graphScores, "graph");
                    List<SearchResult> combined = new ArrayList<>(results);
                    combined.addAll(graphResults);
                    return combined;
                }
            } catch (Exception e) {
                log.warn("Failed to fetch graph associations", e);
            }
        }

        return results;
    }

    @DeleteMapping("/chat/{chatId}/embedding/{windowId}")
    public ResponseEntity<Map<String, String>> deleteEmbedding(@PathVariable String chatId,
                                                                @PathVariable String windowId) {
        try {
            boolean deleted = storageService.deleteEmbedding(chatId, windowId);
            if (deleted) {
                memoryGraphService.removeNode(chatId, windowId);
                return ResponseEntity.ok(Map.of("status", "deleted", "chatId", chatId, "windowId", windowId));
            } else {
                return ResponseEntity.status(404).body(Map.of("status", "not_found", "chatId", chatId, "windowId", windowId));
            }
        } catch (Exception e) {
            log.error("Delete embedding failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/chat/{chatId}")
    public ResponseEntity<Map<String, String>> deleteChat(@PathVariable String chatId) {
        try {
            storageService.deleteChat(chatId);
            return ResponseEntity.ok(Map.of("status", "deleted", "chatId", chatId));
        } catch (Exception e) {
            log.error("Delete failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
