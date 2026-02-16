package com.synapse.embedding.task;

import com.synapse.embedding.config.EmbeddingConfig;
import com.synapse.embedding.service.MemoryGraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GraphDecayTask {

    private static final Logger log = LoggerFactory.getLogger(GraphDecayTask.class);

    private final MemoryGraphService memoryGraphService;
    private final EmbeddingConfig config;

    public GraphDecayTask(MemoryGraphService memoryGraphService, EmbeddingConfig config) {
        this.memoryGraphService = memoryGraphService;
        this.config = config;
    }

    @Scheduled(cron = "${embedding.graph.decay-cron}")
    public void decay() {
        var graphConfig = config.getGraph();
        if (!graphConfig.isEnabled()) {
            return;
        }

        log.info("Starting graph decay task (factor={}, pruneThreshold={})",
                graphConfig.getDecayFactor(), graphConfig.getPruneThreshold());

        memoryGraphService.decayAll(graphConfig.getDecayFactor(), graphConfig.getPruneThreshold());

        log.info("Graph decay task completed");
    }
}
