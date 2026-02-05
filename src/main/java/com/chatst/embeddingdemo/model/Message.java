package com.chatst.embeddingdemo.model;

/**
 * 聊天消息
 */
public record Message(
    String id,           // 消息ID
    String role,         // 角色: user/assistant
    String content,      // 消息内容
    long timestamp       // 时间戳
) {}
