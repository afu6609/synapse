package com.chatst.embeddingdemo.websocket;

import com.chatst.embeddingdemo.model.*;
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
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EmbeddingWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final EmbeddingService embeddingService;
    private final RerankService rerankService;
    private final VectorStorageService storageService;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public EmbeddingWebSocketHandler(ObjectMapper objectMapper,
                                      EmbeddingService embeddingService,
                                      RerankService rerankService,
                                      VectorStorageService storageService) {
        this.objectMapper = objectMapper;
        this.embeddingService = embeddingService;
        this.rerankService = rerankService;
        this.storageService = storageService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("WebSocket connected: {}", session.getId());
        sendMessage(session, Map.of("type", "connected", "sessionId", session.getId()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("WebSocket disconnected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode request = objectMapper.readTree(message.getPayload());
            String action = request.path("action").asText();

            switch (action) {
                case "embed" -> handleEmbed(session, request);
                case "search" -> handleSearch(session, request);
                case "delete" -> handleDelete(session, request);
                case "ping" -> sendMessage(session, Map.of("type", "pong"));
                default -> sendError(session, "Unknown action: " + action);
            }
        } catch (Exception e) {
            log.error("Error handling message", e);
            sendError(session, e.getMessage());
        }
    }

    private void handleEmbed(WebSocketSession session, JsonNode request) throws Exception {
        String chatId = request.path("chatId").asText();
        JsonNode messagesNode = request.path("messages");

        List<Message> messages = objectMapper.readerForListOf(Message.class).readValue(messagesNode);

        // 发送开始通知
        sendMessage(session, Map.of(
                "type", "progress",
                "action", "embed",
                "status", "started",
                "total", messages.size()
        ));

        List<EmbeddingResult> results = embeddingService.embedWithSlidingWindow(chatId, messages);
        storageService.saveEmbeddings(chatId, results);

        // 发送完成通知
        sendMessage(session, Map.of(
                "type", "result",
                "action", "embed",
                "status", "completed",
                "data", results
        ));
    }

    private void handleSearch(WebSocketSession session, JsonNode request) throws Exception {
        String chatId = request.path("chatId").asText();
        String query = request.path("query").asText();
        int topK = request.path("topK").asInt(5);
        boolean useRerank = request.path("useRerank").asBoolean(false);

        List<SearchResult> results = storageService.search(chatId, query, topK * 2);

        if (useRerank && !results.isEmpty()) {
            results = rerankService.rerank(query, results, topK);
        } else {
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
