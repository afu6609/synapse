package com.synapse.embedding.model;

/**
 * 向量搜索请求
 * 使用包装类型以兼容 Native Image 下 JSON 缺省字段反序列化
 */
public record SearchRequest(
        String chatId, // 聊天ID
        String query, // 查询文本
        Integer topK, // 返回前K个结果
        Boolean useRerank, // 是否使用重排序
        Integer nearbyCount, // 附近消息检索数 (如2表示前后各1条)
        Boolean useGraph // 是否使用图关联结果
) {
    public SearchRequest {
        if (topK == null || topK <= 0)
            topK = 5;
        if (useRerank == null)
            useRerank = false;
        if (nearbyCount == null || nearbyCount < 0)
            nearbyCount = 0;
        if (useGraph == null)
            useGraph = false;
    }
}
