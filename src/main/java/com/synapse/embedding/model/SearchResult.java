package com.synapse.embedding.model;

import java.util.List;

/**
 * 搜索结果
 */
public record SearchResult(
    String windowId,
    String content,
    double score,            // 相似度分数
    List<String> messageIds,
    int messageIndex,        // 消息下标
    String matchType         // "vector" | "nearby" | "graph"
) {}
