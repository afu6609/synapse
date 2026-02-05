package com.chatst.embeddingdemo.config;

import com.chatst.embeddingdemo.websocket.EmbeddingWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final EmbeddingWebSocketHandler embeddingWebSocketHandler;

    public WebSocketConfig(EmbeddingWebSocketHandler embeddingWebSocketHandler) {
        this.embeddingWebSocketHandler = embeddingWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(embeddingWebSocketHandler, "/ws/embedding")
                .setAllowedOrigins("*");  // 允许所有来源，生产环境应限制
    }
}
