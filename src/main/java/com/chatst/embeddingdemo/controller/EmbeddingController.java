package com.chatst.embeddingdemo.controller;

import com.chatst.embeddingdemo.model.*;
import com.chatst.embeddingdemo.service.EmbeddingService;
import com.chatst.embeddingdemo.service.RerankService;
import com.chatst.embeddingdemo.service.VectorStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class EmbeddingController {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingController.class);

    private final EmbeddingService embeddingService;
    private final RerankService rerankService;
    private final VectorStorageService storageService;

    public EmbeddingController(EmbeddingService embeddingService,
                               RerankService rerankService,
                               VectorStorageService storageService) {
        this.embeddingService = embeddingService;
        this.rerankService = rerankService;
        this.storageService = storageService;
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "embedding-service"));
    }

    /**
     * 向量化消息 (使用滑动窗口)
     */
    @PostMapping("/embed")
    public ResponseEntity<List<EmbeddingResult>> embed(@RequestBody EmbeddingRequest request) {
        log.info("Embedding request for chat: {}, messages: {}", request.chatId(), request.messages().size());

        try {
            List<EmbeddingResult> results = embeddingService.embedWithSlidingWindow(
                    request.chatId(),
                    request.messages()
            );

            // 保存到存储
            storageService.saveEmbeddings(request.chatId(), results);

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Embedding failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 单条文本向量化 (不保存)
     */
    @PostMapping("/embed/text")
    public ResponseEntity<Map<String, Object>> embedText(@RequestBody Map<String, String> request) {
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

    /**
     * 搜索相似内容
     */
    @PostMapping("/search")
    public ResponseEntity<List<SearchResult>> search(@RequestBody SearchRequest request) {
        log.info("Search request for chat: {}, query: {}", request.chatId(), request.query());

        try {
            List<SearchResult> results = storageService.search(
                    request.chatId(),
                    request.query(),
                    request.topK() * 2  // 如果要rerank，先多取一些
            );

            if (request.useRerank() && !results.isEmpty()) {
                results = rerankService.rerank(request.query(), results, request.topK());
            } else {
                results = results.subList(0, Math.min(request.topK(), results.size()));
            }

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Search failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除聊天的向量数据
     */
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
