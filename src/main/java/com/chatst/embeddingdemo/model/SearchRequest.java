package com.chatst.embeddingdemo.model;

/**
 * 向量搜索请求
 */
public record SearchRequest(
    String chatId,       // 聊天ID
    String query,        // 查询文本
    int topK,            // 返回前K个结果
    boolean useRerank    // 是否使用重排序
) {
    public SearchRequest {
        if (topK <= 0) topK = 5;
    }
}
