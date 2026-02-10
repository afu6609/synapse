package com.chatst.embeddingdemo.websocket;

import com.chatst.embeddingdemo.config.EmbeddingConfig;
import com.chatst.embeddingdemo.model.*;
import com.chatst.embeddingdemo.service.ConfigService;
import com.chatst.embeddingdemo.service.EmbeddingService;
import com.chatst.embeddingdemo.service.RerankService;
import com.chatst.embeddingdemo.service.VectorStorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class EmbeddingWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingWebSocketHandler.class);

    private static final int SEND_TIME_LIMIT = 5000;   // 5s
    private static final int BUFFER_SIZE_LIMIT = 512 * 1024; // 512KB

    private final ObjectMapper objectMapper;
    private final EmbeddingService embeddingService;
    private final RerankService rerankService;
    private final VectorStorageService storageService;
    private final ConfigService configService;
    private final EmbeddingConfig config;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public EmbeddingWebSocketHandler(ObjectMapper objectMapper,
                                     EmbeddingService embeddingService,
                                     RerankService rerankService,
                                     VectorStorageService storageService,
                                     ConfigService configService,
                                     EmbeddingConfig config) {
        this.objectMapper = objectMapper;
        this.embeddingService = embeddingService;
        this.rerankService = rerankService;
        this.storageService = storageService;
        this.configService = configService;
        this.config = config;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketSession decorated = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT, BUFFER_SIZE_LIMIT);
        sessions.put(session.getId(), decorated);
        log.info("WebSocket connected: {}", session.getId());
        sendMessage(decorated, Map.of("type", "connected", "sessionId", session.getId()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("WebSocket disconnected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Look up the decorated session for sending
        WebSocketSession decorated = sessions.get(session.getId());
        if (decorated == null) {
            decorated = session;
        }

        try {
            JsonNode request = objectMapper.readTree(message.getPayload());
            String action = request.path("action").asText();

            switch (action) {
                case "embed" -> handleEmbed(decorated, request);
                case "search" -> handleSearch(decorated, request);
                case "delete" -> handleDelete(decorated, request);
                case "config" -> handleConfig(decorated, request);
                case "ping" -> sendMessage(decorated, Map.of("type", "pong"));
                default -> sendError(decorated, "Unknown action: " + action);
            }
        } catch (Exception e) {
            log.error("Error handling message", e);
            sendError(decorated, e.getMessage());
        }
    }

    private void handleEmbed(WebSocketSession session, JsonNode request) throws Exception {
        if (!config.getProvider().isConfigured()) {
            sendError(session, "Embedding provider is not configured. Please set provider.baseUrl and provider.model first.");
            return;
        }

        String chatId = request.path("chatId").asText();
        boolean useSlidingWindow = request.path("useSlidingWindow").asBoolean(false);
        int windowSize = request.path("windowSize").asInt(2);
        JsonNode messagesNode = request.path("messages");

        List<Message> messages = objectMapper.readerForListOf(Message.class).readValue(messagesNode);

        sendMessage(session, Map.of(
                "type", "progress",
                "action", "embed",
                "status", "started",
                "total", messages.size()
        ));

        List<EmbeddingResult> results;
        if (useSlidingWindow) {
            results = embeddingService.embedWithSlidingWindow(chatId, messages, windowSize);
        } else {
            results = embeddingService.embedIndividually(chatId, messages);
        }

        storageService.saveEmbeddings(chatId, results);

        sendMessage(session, Map.of(
                "type", "result",
                "action", "embed",
                "status", "completed",
                "data", results
        ));
    }

    private void handleSearch(WebSocketSession session, JsonNode request) throws Exception {
        if (!config.getProvider().isConfigured()) {
            sendError(session, "Embedding provider is not configured. Please set provider.baseUrl and provider.model first.");
            return;
        }

        String chatId = request.path("chatId").asText();
        String query = request.path("query").asText();
        int topK = request.path("topK").asInt(5);
        boolean useRerank = request.path("useRerank").asBoolean(false);
        int nearbyCount = request.path("nearbyCount").asInt(0);

        List<SearchResult> results;

        if (nearbyCount > 0) {
            results = storageService.searchWithNearby(chatId, query, topK, nearbyCount);
        } else {
            results = storageService.search(chatId, query, topK * 2);
        }

        if (useRerank && !results.isEmpty()) {
            if (!config.getRerank().isConfigured()) {
                sendError(session, "Rerank provider is not configured. Please set rerank.baseUrl and rerank.model first.");
                return;
            }

            List<SearchResult> matched = results.stream().filter(SearchResult::isMatch).toList();
            List<SearchResult> nearby = results.stream().filter(r -> !r.isMatch()).toList();

            matched = rerankService.rerank(query, matched, topK);

            if (!nearby.isEmpty()) {
                Set<Integer> rerankedIndices = matched.stream()
                        .map(r -> Integer.valueOf(r.messageIndex()))
                        .collect(Collectors.toSet());

                int radius = nearbyCount / 2;
                if (radius <= 0) radius = 1;
                final int r = radius;
                nearby = nearby.stream().filter(n -> {
                    for (Integer idx : rerankedIndices) {
                        if (Math.abs(n.messageIndex() - idx) <= r) return true;
                    }
                    return false;
                }).toList();

                results = new ArrayList<>(matched);
                results.addAll(nearby);
                results.sort(Comparator.comparingInt(SearchResult::messageIndex));
            } else {
                results = matched;
            }
        } else if (nearbyCount <= 0) {
            results = results.subList(0, Math.min(topK, results.size()));
        }

        sendMessage(session, Map.of(
                "type", "result",
                "action", "search",
                "data", results
        ));
    }

    private void handleDelete(WebSocketSession session, JsonNode request) throws Exception {
        String chatId = request.path("chatId").asText();
        storageService.deleteChat(chatId);

        sendMessage(session, Map.of(
                "type", "result",
                "action", "delete",
                "chatId", chatId,
                "status", "deleted"
        ));
    }

    private void handleConfig(WebSocketSession session, JsonNode request) {
        String operation = request.path("operation").asText("get");

        switch (operation) {
            case "get" -> sendMessage(session, Map.of(
                    "type", "result",
                    "action", "config",
                    "data", configService.getConfigSnapshot()
            ));
            case "update" -> {
                JsonNode dataNode = request.path("data");
                @SuppressWarnings("unchecked")
                Map<String, Object> updates = objectMapper.convertValue(dataNode, Map.class);
                List<String> changedFields = configService.applyConfigUpdate(updates);
                sendMessage(session, Map.of(
                        "type", "result",
                        "action", "config",
                        "changedFields", changedFields,
                        "data", configService.getConfigSnapshot()
                ));
            }
            case "detect-dimension" -> {
                try {
                    Integer dimension = configService.detectDimension();
                    sendMessage(session, Map.of(
                            "type", "result",
                            "action", "config",
                            "detectedDimension", dimension,
                            "data", configService.getConfigSnapshot()
                    ));
                } catch (Exception e) {
                    sendError(session, "Dimension detection failed: " + e.getMessage());
                }
            }
            default -> sendError(session, "Unknown config operation: " + operation);
        }
    }

    /**
     * Broadcast a message to all connected WS clients (thread-safe via ConcurrentWebSocketSessionDecorator).
     */
    public void broadcast(Object data) {
        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (IOException e) {
            log.error("Failed to serialize broadcast message", e);
            return;
        }
        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    log.warn("Failed to broadcast to session: {}", session.getId(), e);
                }
            }
        }
    }

    private void sendMessage(WebSocketSession session, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Failed to send message", e);
        }
    }

    private void sendError(WebSocketSession session, String error) {
        sendMessage(session, Map.of("type", "error", "message", error));
    }
}
