# Embedding Service API 文档

基于 Spring Boot 的向量嵌入服务，支持 SiliconFlow 和 Ollama 两种 Embedding 提供商，提供 REST API 和 WebSocket 双协议接口。

**服务端口**: `23456`

---

## 目录

- [REST API](#rest-api)
  - [健康检查](#1-健康检查)
  - [向量化消息](#2-向量化消息)
  - [单条文本向量化](#3-单条文本向量化)
  - [搜索相似内容](#4-搜索相似内容)
  - [删除聊天向量数据](#5-删除聊天向量数据)
  - [获取当前配置](#6-获取当前配置)
  - [更新配置](#7-更新配置)
  - [手动维度检测](#8-手动维度检测)
- [WebSocket API](#websocket-api)
  - [连接](#连接)
  - [embed - 向量化](#action-embed---向量化消息)
  - [search - 搜索](#action-search---搜索相似内容)
  - [delete - 删除](#action-delete---删除聊天数据)
  - [config - 配置管理](#action-config---配置管理)
  - [ping - 心跳](#action-ping---心跳检测)
  - [广播事件](#广播事件)
- [数据模型](#数据模型)
- [配置说明](#配置说明)

---

## REST API

基础路径: `http://localhost:23456/api/v1`

### 1. 健康检查

检查服务是否正常运行。

**请求**

```
GET /api/v1/health
```

**响应**

```json
{
  "status": "ok",
  "service": "embedding-service"
}
```

---

### 2. 向量化消息

对消息列表进行向量化，支持滑动窗口模式和逐条模式，结果自动保存到存储。

**请求**

```
POST /api/v1/embed
Content-Type: application/json
```

**请求体**

```json
{
  "chatId": "chat-001",
  "messages": [
    { "id": "msg1", "role": "user", "content": "你好", "timestamp": 1700000000 },
    { "id": "msg2", "role": "assistant", "content": "你好！有什么可以帮你的？", "timestamp": 1700000001 },
    { "id": "msg3", "role": "user", "content": "介绍一下向量数据库", "timestamp": 1700000002 }
  ],
  "useSlidingWindow": true,
  "windowSize": 2
}
```

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `chatId` | string | 是 | - | 聊天 ID，用于区分不同对话 |
| `messages` | Message[] | 是 | - | 消息列表 |
| `useSlidingWindow` | boolean | 否 | `false` | 是否使用滑动窗口模式 |
| `windowSize` | int | 否 | `2` | 滑动窗口大小，仅 `useSlidingWindow=true` 时生效 |

**响应** `200 OK`

```json
[
  {
    "windowId": "msg1_msg2",
    "content": "[user]: 你好\n---\n[assistant]: 你好！有什么可以帮你的？",
    "vector": [0.123, -0.456, ...],
    "messageIds": ["msg1", "msg2"],
    "messageIndex": 0
  }
]
```

**错误响应** `500 Internal Server Error` — 向量化失败

---

### 3. 单条文本向量化

对单条文本进行向量化，不保存结果，适用于测试和调试。

**请求**

```
POST /api/v1/embed/text
Content-Type: application/json
```

**请求体**

```json
{
  "text": "这是一段测试文本"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `text` | string | 是 | 要向量化的文本，不能为空 |

**响应** `200 OK`

```json
{
  "text": "这是一段测试文本",
  "vector": [0.123, -0.456, ...],
  "dimensions": 1024
}
```

**错误响应** `400 Bad Request`

```json
{
  "error": "text is required"
}
```

---

### 4. 搜索相似内容

在指定聊天的向量数据中搜索相似内容，支持 Rerank 重排序和附近消息检索。

**请求**

```
POST /api/v1/search
Content-Type: application/json
```

**请求体**

```json
{
  "chatId": "chat-001",
  "query": "向量数据库是什么",
  "topK": 5,
  "useRerank": true,
  "nearbyCount": 2
}
```

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `chatId` | string | 是 | - | 聊天 ID |
| `query` | string | 是 | - | 查询文本 |
| `topK` | int | 否 | `5` | 返回前 K 个结果 |
| `useRerank` | boolean | 否 | `false` | 是否使用 Rerank 重排序 |
| `nearbyCount` | int | 否 | `0` | 附近消息检索数（如 2 表示前后各 1 条），0 为不检索附近消息 |

**响应** `200 OK`

```json
[
  {
    "windowId": "msg1_msg2",
    "content": "[user]: 介绍一下向量数据库\n---\n[assistant]: 向量数据库是...",
    "score": 0.9523,
    "messageIds": ["msg1", "msg2"],
    "messageIndex": 2,
    "isMatch": true
  },
  {
    "windowId": "msg3",
    "content": "[user]: 还有什么其他的吗",
    "score": 0.0,
    "messageIds": ["msg3"],
    "messageIndex": 3,
    "isMatch": false
  }
]
```

| 响应字段 | 说明 |
|----------|------|
| `score` | 余弦相似度分数，`isMatch=false` 的附近消息为 `0.0` |
| `isMatch` | `true` = 向量直接命中，`false` = 附近上下文消息 |

**错误响应** `500 Internal Server Error` — 搜索失败

---

### 5. 删除聊天向量数据

删除指定聊天的所有向量数据和元数据。

**请求**

```
DELETE /api/v1/chat/{chatId}
```

| 路径参数 | 类型 | 说明 |
|----------|------|------|
| `chatId` | string | 聊天 ID |

**响应** `200 OK`

```json
{
  "status": "deleted",
  "chatId": "chat-001"
}
```

**错误响应** `500 Internal Server Error` — 删除失败

---

### 6. 获取当前配置

读取当前所有配置项，API Key 自动脱敏显示。

**请求**

```
GET /api/v1/config
```

**响应** `200 OK`

```json
{
  "provider": "ollama",
  "siliconflow.baseUrl": "https://api.siliconflow.cn/v1",
  "siliconflow.apiKey": "sk-w****exlo",
  "siliconflow.embeddingModel": "Qwen/Qwen3-Embedding-8B",
  "siliconflow.rerankModel": "Qwen/Qwen3-Reranker-8B",
  "ollama.baseUrl": "http://localhost:11434",
  "ollama.embeddingModel": "dengcao/Qwen3-Embedding-4B:Q4_K_M",
  "slidingWindow.size": 2,
  "slidingWindow.separator": "\n---\n",
  "storage.basePath": "./data/embedding-service",
  "storage.vectorFileSuffix": ".vec",
  "detectedDimension": null
}
```

---

### 7. 更新配置

部分更新配置项。支持任意组合的字段更新。当模型或 URL 变更时自动触发维度检测；存储路径变更时自动刷新存储服务；所有变更会通过 WebSocket 广播通知已连接的客户端。

**请求**

```
PATCH /api/v1/config
Content-Type: application/json
```

**请求体**

```json
{
  "provider": "siliconflow",
  "siliconflow.embeddingModel": "BAAI/bge-large-en-v1.5"
}
```

**可更新的配置 Key**

| Key | 类型 | 说明 |
|-----|------|------|
| `provider` | string | 当前提供商：`siliconflow` 或 `ollama` |
| `siliconflow.baseUrl` | string | SiliconFlow API 地址 |
| `siliconflow.apiKey` | string | SiliconFlow API 密钥 |
| `siliconflow.embeddingModel` | string | SiliconFlow Embedding 模型 |
| `siliconflow.rerankModel` | string | SiliconFlow Rerank 模型 |
| `ollama.baseUrl` | string | Ollama 服务地址 |
| `ollama.embeddingModel` | string | Ollama Embedding 模型 |
| `slidingWindow.size` | int | 滑动窗口大小 |
| `slidingWindow.separator` | string | 消息分隔符 |
| `storage.basePath` | string | 向量存储路径 |
| `storage.vectorFileSuffix` | string | 向量文件后缀 |

**自动触发行为**

| 变更字段 | 自动触发 |
|----------|----------|
| `provider`、`*.baseUrl`、`*.embeddingModel` | 维度检测（`detectDimension`） |
| `storage.basePath` | 存储路径刷新 |
| 任意字段变更 | WebSocket 广播 `config_changed` |

**响应** `200 OK`

```json
{
  "changedFields": ["provider", "siliconflow.embeddingModel"],
  "config": {
    "provider": "siliconflow",
    "siliconflow.baseUrl": "https://api.siliconflow.cn/v1",
    "siliconflow.apiKey": "sk-w****exlo",
    "siliconflow.embeddingModel": "BAAI/bge-large-en-v1.5",
    "...": "..."
  }
}
```

---

### 8. 手动维度检测

发送测试文本 `"test"` 到当前 Embedding 模型，获取向量维度并更新到配置中。

**请求**

```
POST /api/v1/config/detect-dimension
```

**响应** `200 OK`

```json
{
  "detectedDimension": 1024,
  "config": { "...": "..." }
}
```

**错误响应** `500 Internal Server Error`

```json
{
  "error": "Dimension detection failed: Connection refused"
}
```

---

## WebSocket API

### 连接

```
ws://localhost:23456/ws/embedding
```

连接成功后服务端返回：

```json
{
  "type": "connected",
  "sessionId": "abc123"
}
```

### 通用消息格式

**请求格式**

```json
{
  "action": "<action名称>",
  "...": "其他参数"
}
```

**成功响应格式**

```json
{
  "type": "result",
  "action": "<action名称>",
  "data": { "..." }
}
```

**错误响应格式**

```json
{
  "type": "error",
  "message": "错误信息"
}
```

---

### action: embed — 向量化消息

**请求**

```json
{
  "action": "embed",
  "chatId": "chat-001",
  "useSlidingWindow": true,
  "windowSize": 2,
  "messages": [
    { "id": "msg1", "role": "user", "content": "你好", "timestamp": 1700000000 },
    { "id": "msg2", "role": "assistant", "content": "你好！", "timestamp": 1700000001 }
  ]
}
```

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `chatId` | string | 是 | - | 聊天 ID |
| `messages` | Message[] | 是 | - | 消息列表 |
| `useSlidingWindow` | boolean | 否 | `false` | 是否使用滑动窗口 |
| `windowSize` | int | 否 | `2` | 窗口大小 |

**响应 — 进度通知**

```json
{
  "type": "progress",
  "action": "embed",
  "status": "started",
  "total": 2
}
```

**响应 — 完成**

```json
{
  "type": "result",
  "action": "embed",
  "status": "completed",
  "data": [
    {
      "windowId": "msg1_msg2",
      "content": "[user]: 你好\n---\n[assistant]: 你好！",
      "vector": [0.123, ...],
      "messageIds": ["msg1", "msg2"],
      "messageIndex": 0
    }
  ]
}
```

---

### action: search — 搜索相似内容

**请求**

```json
{
  "action": "search",
  "chatId": "chat-001",
  "query": "你好",
  "topK": 5,
  "useRerank": false,
  "nearbyCount": 0
}
```

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `chatId` | string | 是 | - | 聊天 ID |
| `query` | string | 是 | - | 查询文本 |
| `topK` | int | 否 | `5` | 返回前 K 个 |
| `useRerank` | boolean | 否 | `false` | 是否重排序 |
| `nearbyCount` | int | 否 | `0` | 附近消息数 |

**响应**

```json
{
  "type": "result",
  "action": "search",
  "data": [
    {
      "windowId": "msg1_msg2",
      "content": "...",
      "score": 0.95,
      "messageIds": ["msg1", "msg2"],
      "messageIndex": 0,
      "isMatch": true
    }
  ]
}
```

---

### action: delete — 删除聊天数据

**请求**

```json
{
  "action": "delete",
  "chatId": "chat-001"
}
```

**响应**

```json
{
  "type": "result",
  "action": "delete",
  "chatId": "chat-001",
  "status": "deleted"
}
```

---

### action: config — 配置管理

通过 `operation` 字段区分操作类型。

#### operation: get — 读取配置

**请求**

```json
{
  "action": "config",
  "operation": "get"
}
```

**响应**

```json
{
  "type": "result",
  "action": "config",
  "data": {
    "provider": "ollama",
    "siliconflow.baseUrl": "https://api.siliconflow.cn/v1",
    "siliconflow.apiKey": "sk-w****exlo",
    "...": "..."
  }
}
```

#### operation: update — 更新配置

**请求**

```json
{
  "action": "config",
  "operation": "update",
  "data": {
    "provider": "siliconflow",
    "siliconflow.embeddingModel": "BAAI/bge-large-en-v1.5"
  }
}
```

**响应**

```json
{
  "type": "result",
  "action": "config",
  "changedFields": ["provider", "siliconflow.embeddingModel"],
  "data": { "...完整配置快照..." }
}
```

#### operation: detect-dimension — 维度检测

**请求**

```json
{
  "action": "config",
  "operation": "detect-dimension"
}
```

**响应**

```json
{
  "type": "result",
  "action": "config",
  "detectedDimension": 1024,
  "data": { "...完整配置快照..." }
}
```

**错误响应**

```json
{
  "type": "error",
  "message": "Dimension detection failed: Connection refused"
}
```

---

### action: ping — 心跳检测

**请求**

```json
{
  "action": "ping"
}
```

**响应**

```json
{
  "type": "pong"
}
```

---

### 广播事件

当配置通过 REST API 或 WebSocket 更新时，所有已连接的 WebSocket 客户端会收到广播消息：

#### config_changed — 配置变更通知

```json
{
  "type": "config_changed",
  "changedFields": ["provider", "siliconflow.embeddingModel"],
  "config": {
    "provider": "siliconflow",
    "siliconflow.baseUrl": "https://api.siliconflow.cn/v1",
    "...": "..."
  }
}
```

---

## 数据模型

### Message — 聊天消息

```json
{
  "id": "msg1",
  "role": "user",
  "content": "消息内容",
  "timestamp": 1700000000
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 消息唯一 ID |
| `role` | string | 角色：`user` 或 `assistant` |
| `content` | string | 消息内容 |
| `timestamp` | long | 时间戳 |

### EmbeddingResult — 向量化结果

```json
{
  "windowId": "msg1_msg2",
  "content": "[user]: 你好\n---\n[assistant]: 你好！",
  "vector": [0.123, -0.456, 0.789, "..."],
  "messageIds": ["msg1", "msg2"],
  "messageIndex": 0
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `windowId` | string | 窗口/消息 ID |
| `content` | string | 组合后的文本内容 |
| `vector` | float[] | 向量数组 |
| `messageIds` | string[] | 包含的消息 ID 列表 |
| `messageIndex` | int | 在消息列表中的下标位置 |

### SearchResult — 搜索结果

```json
{
  "windowId": "msg1_msg2",
  "content": "...",
  "score": 0.9523,
  "messageIds": ["msg1", "msg2"],
  "messageIndex": 2,
  "isMatch": true
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `windowId` | string | 窗口/消息 ID |
| `content` | string | 文本内容 |
| `score` | double | 余弦相似度分数（附近消息为 `0.0`） |
| `messageIds` | string[] | 消息 ID 列表 |
| `messageIndex` | int | 消息下标 |
| `isMatch` | boolean | `true` = 向量直接命中，`false` = 附近上下文 |

---

## 配置说明

配置文件路径: `src/main/resources/application.yml`

```yaml
server:
  port: 23456

embedding:
  provider: ollama                    # 当前提供商: siliconflow | ollama

  siliconflow:
    base-url: https://api.siliconflow.cn/v1
    api-key: ${SILICONFLOW_API_KEY:your-key}
    embedding-model: Qwen/Qwen3-Embedding-8B
    rerank-model: Qwen/Qwen3-Reranker-8B

  ollama:
    base-url: http://localhost:11434
    embedding-model: dengcao/Qwen3-Embedding-4B:Q4_K_M

  sliding-window:
    size: 2                           # 滑动窗口大小
    separator: "\n---\n"              # 消息分隔符

  storage:
    base-path: ./data/embedding-service   # 向量存储路径（项目当前目录）
    vector-file-suffix: .vec
```

所有配置项均支持通过 `PATCH /api/v1/config` 或 WebSocket `config.update` 在运行时动态修改，无需重启服务。
