package com.chatst.embeddingdemo.controller;

import com.chatst.embeddingdemo.config.EmbeddingConfig;
import com.chatst.embeddingdemo.model.*;
import com.chatst.embeddingdemo.service.EmbeddingService;
import com.chatst.embeddingdemo.service.RerankService;
import com.chatst.embeddingdemo.service.VectorStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class EmbeddingController {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingController.class);

    private final EmbeddingService embeddingService;
    private final RerankService rerankService;
    private final VectorStorageService storageService;
    private final EmbeddingConfig config;

    public EmbeddingController(EmbeddingService embeddingService,
                               RerankService rerankService,
                               VectorStorageService storageService,
                               EmbeddingConfig config) {
        this.embeddingService = embeddingService;
        this.rerankService = rerankService;
        this.storageService = storageService;
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

        log.info("Search request for chat: {}, query: {}, nearbyCount: {}",
                request.chatId(), request.query(), request.nearbyCount());

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

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Search failed", e);
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
