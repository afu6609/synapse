package com.chatst.embeddingdemo.model;

import java.util.List;

/**
 * 向量化请求
 */
public record EmbeddingRequest(
    String chatId,           // 聊天ID (用于区分不同对话)
    List<Message> messages,  // 要向量化的消息列表
    boolean useSlidingWindow, // 是否使用滑动窗口
    int windowSize           // 滑动窗口大小，仅useSlidingWindow=true时有效
) {
    public EmbeddingRequest {
        if (windowSize <= 0) windowSize = 2;
    }
}
