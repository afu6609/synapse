package com.chatst.embeddingdemo.model;

import java.util.List;

/**
 * 向量化请求
 * 使用包装类型以兼容 Native Image 下 JSON 缺省字段反序列化
 */
public record EmbeddingRequest(
        String chatId, // 聊天ID (用于区分不同对话)
        List<Message> messages, // 要向量化的消息列表
        Boolean useSlidingWindow, // 是否使用滑动窗口
        Integer windowSize // 滑动窗口大小，仅useSlidingWindow=true时有效
) {
    public EmbeddingRequest {
        if (useSlidingWindow == null)
            useSlidingWindow = false;
        if (windowSize == null || windowSize <= 0)
            windowSize = 2;
    }
}
