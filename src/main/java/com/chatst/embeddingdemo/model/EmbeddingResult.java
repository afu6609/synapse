package com.chatst.embeddingdemo.model;

import java.util.List;

/**
 * 向量化结果
 */
public record EmbeddingResult(
    String windowId,         // 窗口ID (如 "msg1_msg2")
    String content,          // 组合后的内容
    List<Float> vector,      // 向量
    List<String> messageIds, // 包含的消息ID列表
    int messageIndex         // 消息下标
) {}
