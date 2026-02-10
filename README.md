# Embedding Service

基于 Spring Boot 4.0.2 的向量嵌入服务，支持所有 OpenAI 兼容 API 的 Embedding/Rerank 提供商（Ollama、SiliconFlow、OpenAI、Jina 等），所有配置均可运行时动态修改。提供 REST API 和 WebSocket 双协议接口。支持 GraalVM native image 编译。

**服务端口**: `23456`

---

## 目录

- [快速开始](#快速开始)
- [GraalVM Native Image 构建](#graalvm-native-image-构建)
- [运行时配置](#运行时配置)
- [REST API](#rest-api)
- [WebSocket API](#websocket-api)
- [数据模型](#数据模型)

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
| `provider.baseUrl` | string | Embedding API 地址（如 `http://localhost:11434/v1`） |
| `provider.model` | string | Embedding 模型名 |
| `provider.apiKey` | string | Embedding API 密钥（可选，无密钥时不传即可） |
| `rerank.baseUrl` | string | Rerank API 地址 |
| `rerank.model` | string | Rerank 模型名 |
| `rerank.apiKey` | string | Rerank API 密钥（可选） |
| `slidingWindow.size` | int | 滑动窗口大小（默认 2） |
| `slidingWindow.separator` | string | 消息分隔符（默认 `\n---\n`） |
| `storage.basePath` | string | 向量存储路径（默认 `./data/embedding-service`） |
| `storage.vectorFileSuffix` | string | 向量文件后缀（默认 `.vec`） |

### 自动触发行为

| 变更字段 | 自动触发 |
|----------|----------|
| `provider.baseUrl`、`provider.model` | 自动检测 Embedding 维度 |
| `storage.basePath` | 刷新存储路径 |
| 任意字段变更 | WebSocket 广播 `config_changed` 事件 |

### `application.yml` 默认配置

只保留非 provider 的静态配置：

```yaml
server:
  port: 23456

spring:
  threads:
    virtual:
      enabled: true

embedding:
  sliding-window:
    size: 2
    separator: "\n---\n"
  storage:
    base-path: ./data/embedding-service
    vector-file-suffix: .vec
```

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

在指定聊天的向量数据中搜索相似内容，支持 Rerank 重排序和附近消息检索。

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
  "nearbyCount": 2
}
```

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `chatId` | string | 是 | - | 聊天 ID |
| `query` | string | 是 | - | 查询文本 |
| `topK` | int | 否 | `5` | 返回前 K 个结果 |
| `useRerank` | boolean | 否 | `false` | 是否使用 Rerank 重排序 |
| `nearbyCount` | int | 否 | `0` | 附近消息检索数（如 2 表示前后各 1 条） |

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

---

### 5. 删除聊天向量数据

```
DELETE /api/v1/chat/{chatId}
```

```json
{ "status": "deleted", "chatId": "chat-001" }
```

---

### 6. 获取当前配置

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
  "slidingWindow.size": 2,
  "slidingWindow.separator": "\n---\n",
  "storage.basePath": "./data/embedding-service",
  "storage.vectorFileSuffix": ".vec",
  "detectedDimension": 1024
}
```

---

### 7. 更新配置

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

### 8. 手动维度检测

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
  "nearbyCount": 0
}
```

---

### action: delete — 删除聊天数据

```json
{ "action": "delete", "chatId": "chat-001" }
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
  "isMatch": true
}
```

| 字段 | 说明 |
|------|------|
| `score` | 余弦相似度；`isMatch=false` 的附近消息为 `0.0` |
| `isMatch` | `true` = 向量直接命中，`false` = 附近上下文 |
