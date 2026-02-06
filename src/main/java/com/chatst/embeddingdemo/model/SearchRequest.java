package com.chatst.embeddingdemo.model;

/**
 * 向量搜索请求
 */
public record SearchRequest(
    String chatId,       // 聊天ID
    String query,        // 查询文本
    int topK,            // 返回前K个结果
    boolean useRerank,   // 是否使用重排序
    int nearbyCount      // 附近消息检索数 (如2表示前后各1条)
) {
    public SearchRequest {
        if (topK <= 0) topK = 5;
        if (nearbyCount < 0) nearbyCount = 0;
    }
}
