# Embedding Service

基于 Spring Boot 4.0.2 的向量嵌入服务，支持所有 OpenAI 兼容 API 的 Embedding/Rerank 提供商（Ollama、SiliconFlow、OpenAI、Jina 等），也支持本地 ONNX 嵌入模型（无需外部服务）。所有配置均可运行时动态修改。提供 REST API 和 WebSocket 双协议接口。支持 GraalVM native image 编译。

**新功能**: 记忆关联拓扑图（搜索时自动建立共激活关系，激活关联记忆）、单条嵌入精确删除。

**服务端口**: `23456`

---

## 目录

- [快速开始](#快速开始)
- [GraalVM Native Image 构建](#graalvm-native-image-构建)
- [运行时配置](#运行时配置)
- [REST API](#rest-api)
- [WebSocket API](#websocket-api)
- [数据模型](#数据模型)
- [记忆关联图](#记忆关联图)

---

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- （可选）GraalVM 25 for JDK 25（打包 native image）

### 启动服务

```bash
mvn spring-boot:run
```

服务启动后**不需要**预先配置 provider，可以通过 API 在运行时动态配置。

### 配置 Embedding 提供商

启动后通过 HTTP API 配置 provider。所有提供商统一使用 OpenAI 兼容 API 格式，没有密钥就不用传 `apiKey`。

**Ollama（本地）：**

```bash
curl -X PATCH http://localhost:23456/api/v1/config \
  -H "Content-Type: application/json" \
  -d '{
    "provider.baseUrl": "http://localhost:11434/v1",
    "provider.model": "qwen3-embedding-4b"
  }'
```

> Ollama 的 baseUrl 需要加 `/v1` 后缀以使用其 OpenAI 兼容端点。

**SiliconFlow 等云端 API：**

```bash
curl -X PATCH http://localhost:23456/api/v1/config \
  -H "Content-Type: application/json" \
  -d '{
    "provider.baseUrl": "https://api.siliconflow.cn/v1",
    "provider.model": "Qwen/Qwen3-Embedding-8B",
    "provider.apiKey": "sk-your-api-key"
  }'
```

### 配置 Rerank 提供商（可选）

```bash
curl -X PATCH http://localhost:23456/api/v1/config \
  -H "Content-Type: application/json" \
  -d '{
    "rerank.baseUrl": "https://api.siliconflow.cn/v1",
    "rerank.model": "Qwen/Qwen3-Reranker-8B",
    "rerank.apiKey": "sk-your-api-key"
  }'
```

### 配置本地嵌入模型（无需外部服务）

内置 ONNX 本地嵌入模型，无需运行 Ollama 或其他外部 API 即可生成向量。

```bash
curl -X PATCH http://localhost:23456/api/v1/config \
  -H "Content-Type: application/json" \
  -d '{
    "provider.type": "local",
    "provider.model": "bge-small-zh-v15"
  }'
```

支持的本地模型：

| 模型标识 | 说明 | 维度 |
|----------|------|------|
| `bge-small-zh-v15` | BGE Small Chinese v1.5（ONNX） | 512 |

切回 API 模式：

```bash
curl -X PATCH http://localhost:23456/api/v1/config \
  -H "Content-Type: application/json" \
  -d '{
    "provider.type": "api",
    "provider.baseUrl": "http://localhost:11434/v1",
    "provider.model": "qwen3-embedding-4b"
  }'
```

配置完成后即可使用 embed/search 接口。维度会在 provider 配置变更时自动检测。

---

## GraalVM Native Image 构建

### 环境要求

- GraalVM 25（JDK 25）或 GraalVM for JDK 21
- 如使用 GraalVM for JDK 25，需要将 `pom.xml` 中 `<java.version>` 改为 `25`

### 构建命令

```bash
mvn -Pnative native:compile
```

构建产物在 `target/` 目录下，可直接运行：

```bash
./target/embeddingDemo
```

### 已处理的兼容性问题

| 组件 | 问题 | 处理方式 |
|------|------|----------|
| Java Record 模型 | WebSocket 中使用 ObjectMapper 手动序列化 record，AOT 可能未自动检测 | `@RegisterReflectionForBinding` 显式注册 |
| RestTemplate | 手动 `new RestTemplate()` 绕过 AOT 检测 | 改为 Spring Bean 注入 |
| sqlite-jdbc 3.49.1.0 | JNI 本地库加载 | 库自带 `META-INF/native-image/` 元数据，自动支持 |
| `@EnableAsync` + `@EventListener` | CGLIB 代理 | Spring Boot 4.0 AOT 自动处理 |
| WebSocket | `ConcurrentWebSocketSessionDecorator` | Spring WebSocket 模块已包含 native 支持 |
| 虚拟线程 | `spring.threads.virtual.enabled=true` | GraalVM 25 支持，如遇问题可在 yml 中关闭 |

### 排查 native image 问题

如遇运行时反射错误，可用 tracing agent 收集缺失的元数据：

```bash
java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image -jar target/embeddingDemo-0.1.jar
```

执行所有功能后停止，会在 `META-INF/native-image/` 下生成 `reflect-config.json` 等文件，再重新 `native:compile`。

---

## 运行时配置

所有 provider 配置均**不在** `application.yml` 中硬编码，通过 REST API 或 WebSocket 在运行时动态设置。

### 可配置项

| Key | 类型 | 说明 |
|-----|------|------|
| `provider.type` | string | 提供商类型：`api`（默认，外部 HTTP API）或 `local`（本地 ONNX 模型） |
| `provider.baseUrl` | string | Embedding API 地址（如 `http://localhost:11434/v1`），`local` 模式不需要 |
| `provider.model` | string | Embedding 模型名（`local` 模式下为本地模型标识，如 `bge-small-zh-v15`） |
| `provider.apiKey` | string | Embedding API 密钥（可选，无密钥时不传即可） |
| `rerank.baseUrl` | string | Rerank API 地址 |
| `rerank.model` | string | Rerank 模型名 |
| `rerank.apiKey` | string | Rerank API 密钥（可选） |
| `chunk.enabled` | boolean | 是否启用长消息分块（默认 `true`） |
| `chunk.maxLength` | int | 触发分块的字符数阈值（默认 `512`） |
| `slidingWindow.size` | int | 滑动窗口大小（默认 2） |
| `slidingWindow.separator` | string | 消息分隔符（默认 `\n---\n`） |
| `storage.basePath` | string | 向量存储路径（默认 `./data/embedding-service`） |
| `storage.vectorFileSuffix` | string | 向量文件后缀（默认 `.vec`） |
| `graph.enabled` | boolean | 是否启用记忆关联图（默认 `true`） |
| `graph.decayFactor` | double | 衰减因子，每次定时任务 weight *= factor（默认 `0.95`） |
| `graph.pruneThreshold` | double | 剪枝阈值，低于此值的边被删除（默认 `0.01`） |
| `graph.queryThreshold` | double | 查询阈值，weight >= threshold 的关联才返回（默认 `0.5`） |
| `graph.maxGraphResults` | int | 图关联最多返回条数（默认 `5`） |
| `graph.decayCron` | string | 衰减定时任务 cron 表达式（默认 `0 0 3 * * *`，每天凌晨 3 点） |

### 自动触发行为

| 变更字段 | 自动触发 |
|----------|----------|
| `provider.type`、`provider.baseUrl`、`provider.model` | 自动检测 Embedding 维度 |
| `storage.basePath` | 刷新存储路径 |
| 任意字段变更 | WebSocket 广播 `config_changed` 事件 |

### `application.yml` 默认配置

```yaml
server:
  port: 23456

spring:
  threads:
    virtual:
      enabled: true

embedding:
  chunk:
    enabled: true
    max-length: 512
  sliding-window:
    size: 2
    separator: "\n---\n"
  storage:
    base-path: ./data/embedding-service
    vector-file-suffix: .vec
  graph:
    enabled: true
    decay-factor: 0.95
    prune-threshold: 0.01
    query-threshold: 0.5
    max-graph-results: 5
    decay-cron: "0 0 3 * * *"
```

---

## 长消息分块（Chunking）

当消息内容超过 `chunk.maxLength`（默认 512 字符）时，自动按段落切分为多个 chunk，每个 chunk 独立向量化。

### 分块规则

1. **短消息**（≤ maxLength）：整条存为一个 chunk，格式 `[role]: content`
2. **长消息**：按 `\n\n` 段落切分 → 合并小段落 → 超长段落按 `\n` 二次切分 → 仍超长则硬切
3. **上下文锚点**：assistant 回复的 chunk 自动附带最近 user 问题（截取前 200 字）作为锚点

### 示例

输入 4 条消息（msg1 为长回复）：
```
msg0 (user): "量子计算是什么？"
msg1 (assistant): "[1500字长回复，3个段落]"
msg2 (user): "有什么应用？"
msg3 (assistant): "[100字短回复]"
```

分块结果（maxLength=512）：
```
chunk 0: "[user]: 量子计算是什么？"
chunk 1: "[Question]: 量子计算是什么？\n---\n[assistant]: 段落1..."
chunk 2: "[Question]: 量子计算是什么？\n---\n[assistant]: 段落2..."
chunk 3: "[Question]: 量子计算是什么？\n---\n[assistant]: 段落3..."
chunk 4: "[user]: 有什么应用？"
chunk 5: "[assistant]: 短回复内容"
```

滑动窗口和 nearby 搜索均在 chunk 级别操作，逻辑不变。设置 `chunk.enabled=false` 可禁用分块，退化为原始行为。

---

## REST API

基础路径: `http://localhost:23456/api/v1`

### 1. 健康检查

```
GET /api/v1/health
```

```json
{ "status": "ok", "service": "embedding-service" }
```

---

### 2. 向量化消息

对消息列表进行向量化，支持滑动窗口模式和逐条模式，结果自动保存。

**需要先配置 `provider.*`，否则返回 400。**

```
POST /api/v1/embed
Content-Type: application/json
```

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
| `chatId` | string | 是 | - | 聊天 ID |
| `messages` | Message[] | 是 | - | 消息列表 |
| `useSlidingWindow` | boolean | 否 | `false` | 是否使用滑动窗口模式 |
| `windowSize` | int | 否 | `2` | 滑动窗口大小 |

**响应** `200 OK`

```json
[
  {
    "windowId": "msg1_msg2",
    "content": "[user]: 你好\n---\n[assistant]: 你好！有什么可以帮你的？",
    "vector": [0.123, -0.456, "..."],
    "messageIds": ["msg1", "msg2"],
    "messageIndex": 0
  }
]
```

---

### 3. 单条文本向量化

对单条文本进行向量化，不保存结果，适用于测试。

```
POST /api/v1/embed/text
Content-Type: application/json
```

```json
{ "text": "这是一段测试文本" }
```

**响应** `200 OK`

```json
{
  "text": "这是一段测试文本",
  "vector": [0.123, -0.456, "..."],
  "dimensions": 1024
}
```

---

### 4. 搜索相似内容

在指定聊天的向量数据中搜索相似内容，支持 Rerank 重排序、附近消息检索和记忆关联图。

**需要先配置 `provider.*`；使用 Rerank 还需配置 `rerank.*`。**

```
POST /api/v1/search
Content-Type: application/json
```

```json
{
  "chatId": "chat-001",
  "query": "向量数据库是什么",
  "topK": 5,
  "useRerank": true,
  "nearbyCount": 2,
  "useGraph": true
}
```

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `chatId` | string | 是 | - | 聊天 ID |
| `query` | string | 是 | - | 查询文本 |
| `topK` | int | 否 | `5` | 返回前 K 个结果 |
| `useRerank` | boolean | 否 | `false` | 是否使用 Rerank 重排序 |
| `nearbyCount` | int | 否 | `0` | 附近消息检索数（如 2 表示前后各 1 条） |
| `useGraph` | boolean | 否 | `false` | 是否在结果中包含图关联记忆 |

**响应** `200 OK`

```json
[
  {
    "windowId": "msg1_msg2",
    "content": "[user]: 介绍一下向量数据库\n---\n[assistant]: 向量数据库是...",
    "score": 0.9523,
    "messageIds": ["msg1", "msg2"],
    "messageIndex": 2,
    "matchType": "vector"
  },
  {
    "windowId": "msg3",
    "content": "[user]: 还有什么其他的吗",
    "score": 0.0,
    "messageIds": ["msg3"],
    "messageIndex": 3,
    "matchType": "nearby"
  },
  {
    "windowId": "msg5_msg6",
    "content": "[user]: 数据库对比...",
    "score": 3.5,
    "messageIds": ["msg5", "msg6"],
    "messageIndex": 5,
    "matchType": "graph"
  }
]
```

| 响应字段 | 说明 |
|----------|------|
| `score` | `vector`: 余弦相似度；`nearby`: `0.0`；`graph`: 图权重 |
| `matchType` | `"vector"` = 向量直接命中，`"nearby"` = 附近上下文，`"graph"` = 图关联激活 |

---

### 5. 删除单条嵌入

按 windowId 精确删除一条嵌入，同时清理该节点在记忆关联图中的所有边。

```
DELETE /api/v1/chat/{chatId}/embedding/{windowId}
```

**成功** `200 OK`

```json
{ "status": "deleted", "chatId": "chat-001", "windowId": "msg1_msg2" }
```

**未找到** `404`

```json
{ "status": "not_found", "chatId": "chat-001", "windowId": "msg1_msg2" }
```

---

### 6. 删除聊天向量数据

删除整个会话的所有数据（嵌入 + 图 + 向量文件）。

```
DELETE /api/v1/chat/{chatId}
```

```json
{ "status": "deleted", "chatId": "chat-001" }
```

---

### 7. 获取当前配置

读取当前所有配置项，API Key 自动脱敏。

```
GET /api/v1/config
```

```json
{
  "provider.baseUrl": "http://localhost:11434/v1",
  "provider.model": "qwen3-embedding-4b",
  "provider.apiKey": "****",
  "rerank.baseUrl": null,
  "rerank.model": null,
  "rerank.apiKey": "****",
  "chunk.enabled": true,
  "chunk.maxLength": 512,
  "slidingWindow.size": 2,
  "slidingWindow.separator": "\n---\n",
  "storage.basePath": "./data/embedding-service",
  "storage.vectorFileSuffix": ".vec",
  "graph.enabled": true,
  "graph.decayFactor": 0.95,
  "graph.pruneThreshold": 0.01,
  "graph.queryThreshold": 0.5,
  "graph.maxGraphResults": 5,
  "graph.decayCron": "0 0 3 * * *",
  "detectedDimension": 1024
}
```

---

### 8. 更新配置

部分更新配置项。支持任意字段组合。变更会通过 WebSocket 广播通知所有已连接的客户端。

```
PATCH /api/v1/config
Content-Type: application/json
```

```json
{
  "provider.baseUrl": "https://api.siliconflow.cn/v1",
  "provider.model": "Qwen/Qwen3-Embedding-8B",
  "provider.apiKey": "sk-xxx"
}
```

**响应** `200 OK`

```json
{
  "changedFields": ["provider.baseUrl", "provider.model", "provider.apiKey"],
  "config": { "...完整配置快照..." }
}
```

---

### 9. 手动维度检测

```
POST /api/v1/config/detect-dimension
```

```json
{
  "detectedDimension": 1024,
  "config": { "..." }
}
```

---

## WebSocket API

### 连接

```
ws://localhost:23456/ws/embedding
```

连接成功后返回：

```json
{ "type": "connected", "sessionId": "abc123" }
```

### 通用消息格式

**请求**

```json
{ "action": "<action>", "...": "其他参数" }
```

**成功响应**

```json
{ "type": "result", "action": "<action>", "data": { "..." } }
```

**错误响应**

```json
{ "type": "error", "message": "错误信息" }
```

---

### action: embed — 向量化消息

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

**进度通知** → **完成通知**

```json
{ "type": "progress", "action": "embed", "status": "started", "total": 2 }
```

```json
{ "type": "result", "action": "embed", "status": "completed", "data": [ "..." ] }
```

---

### action: search — 搜索相似内容

```json
{
  "action": "search",
  "chatId": "chat-001",
  "query": "你好",
  "topK": 5,
  "useRerank": false,
  "nearbyCount": 0,
  "useGraph": true
}
```

响应中每个结果的 `matchType` 字段标识来源：`"vector"` / `"nearby"` / `"graph"`。

---

### action: delete — 删除数据

**删除整个会话：**

```json
{ "action": "delete", "chatId": "chat-001" }
```

**删除单条嵌入：**

```json
{ "action": "delete", "chatId": "chat-001", "windowId": "msg1_msg2" }
```

有 `windowId` 时执行单条删除（并清理图边），无 `windowId` 时删除整个会话。

单条删除响应：

```json
{ "type": "result", "action": "delete", "chatId": "chat-001", "windowId": "msg1_msg2", "status": "deleted" }
```

```json
{ "type": "result", "action": "delete", "chatId": "chat-001", "windowId": "msg1_msg2", "status": "not_found" }
```

---

### action: config — 配置管理

#### get — 读取配置

```json
{ "action": "config", "operation": "get" }
```

#### update — 更新配置

```json
{
  "action": "config",
  "operation": "update",
  "data": {
    "provider.baseUrl": "http://localhost:11434/v1",
    "provider.model": "qwen3-embedding-4b"
  }
}
```

#### detect-dimension — 维度检测

```json
{ "action": "config", "operation": "detect-dimension" }
```

---

### action: ping — 心跳

```json
{ "action": "ping" }
```

```json
{ "type": "pong" }
```

---

### 广播事件

配置更新时，所有已连接的 WebSocket 客户端会异步收到广播：

```json
{
  "type": "config_changed",
  "changedFields": ["provider.baseUrl", "provider.model"],
  "config": { "...完整配置快照..." }
}
```

---

## 数据模型

### Message

```json
{ "id": "msg1", "role": "user", "content": "消息内容", "timestamp": 1700000000 }
```

### EmbeddingResult

```json
{
  "windowId": "msg1_msg2",
  "content": "[user]: 你好\n---\n[assistant]: 你好！",
  "vector": [0.123, -0.456, "..."],
  "messageIds": ["msg1", "msg2"],
  "messageIndex": 0
}
```

### SearchResult

```json
{
  "windowId": "msg1_msg2",
  "content": "...",
  "score": 0.9523,
  "messageIds": ["msg1", "msg2"],
  "messageIndex": 2,
  "matchType": "vector"
}
```

| 字段 | 说明 |
|------|------|
| `score` | `vector`: 余弦相似度；`nearby`: `0.0`；`graph`: 图权重 |
| `matchType` | `"vector"` = 向量直接命中，`"nearby"` = 附近上下文，`"graph"` = 图关联 |

---

## 记忆关联图

记忆关联图是一个基于"共激活"原理的被动学习系统。当多条记忆在搜索中同时被向量命中时，它们之间的关联边会自动增强；长期不共同出现的边会衰减消失。

### 工作原理

1. **被动学习**：每次搜索时，如果 `graph.enabled=true`，所有 `matchType="vector"` 的直接命中结果会被两两记录为共激活对。weight 每次 +1.0。
2. **关联激活**：搜索时如果 `useGraph=true`，会查询直接命中节点的关联节点（weight >= queryThreshold），将关联到的记忆追加到搜索结果中（`matchType="graph"`）。
3. **自动衰减**：定时任务（默认每天凌晨 3 点）对所有 chatId 执行 `weight *= decayFactor`，并删除 `weight < pruneThreshold` 的边。
4. **节点清理**：删除单条嵌入时自动删除该节点在图中的所有边。

### 图数据存储

图数据保存在与嵌入相同的 per-chat `metadata.db` 中（`memory_graph` 表），无需额外存储。

### 配置示例

```bash
# 调整图关联参数
curl -X PATCH http://localhost:23456/api/v1/config \
  -H "Content-Type: application/json" \
  -d '{
    "graph.enabled": true,
    "graph.queryThreshold": 0.5,
    "graph.maxGraphResults": 5
  }'

# 关闭图关联功能
curl -X PATCH http://localhost:23456/api/v1/config \
  -H "Content-Type: application/json" \
  -d '{"graph.enabled": false}'
```
