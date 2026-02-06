package com.chatst.embeddingdemo.model;

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
    boolean isMatch          // 是否为向量命中 (true=直接匹配, false=附近上下文)
) {}
