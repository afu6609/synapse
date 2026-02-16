package com.synapse.embedding.listener;

import com.synapse.embedding.event.ConfigChangedEvent;
import com.synapse.embedding.websocket.EmbeddingWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ConfigChangeBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(ConfigChangeBroadcaster.class);

    private final EmbeddingWebSocketHandler webSocketHandler;

    public ConfigChangeBroadcaster(EmbeddingWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Async
    @EventListener
    public void onConfigChanged(ConfigChangedEvent event) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "config_changed");
        message.put("changedFields", event.getChangedFields());
        message.put("config", event.getConfigSnapshot());

        log.info("Broadcasting config change to WS clients, changed fields: {}", event.getChangedFields());
        webSocketHandler.broadcast(message);
    }
}
