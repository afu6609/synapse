# 项目结构说明

## 概述

embeddingDemo 是一个基于 Spring Boot 4.0.2 的向量嵌入服务，支持：
- 运行时动态配置 embedding 和 rerank 提供商
- 长消息自动分块（Chunking）+ 上下文锚点
- 滑动窗口向量化（在 chunk 级别滑动）
- 向量搜索 + 重排序 + 附近消息检索
- WebSocket 实时通信
- GraalVM Native Image 原生镜像编译

技术栈：Spring Boot 4.0.2 (Spring Framework 7.x), SQLite, GraalVM 25, Virtual Threads

---

## 目录结构

```
embeddingDemo/
├── src/main/java/com/chatst/embeddingdemo/
│   ├── EmbeddingDemoApplication.java        # 主启动类
│   │
│   ├── config/                              # 配置类
│   │   ├── AsyncConfig.java                 # 启用异步事件处理
│   │   ├── EmbeddingConfig.java             # 核心配置（embedding/rerank 提供商）
│   │   ├── HttpClientConfig.java            # RestTemplate Bean 配置
│   │   ├── JacksonConfig.java               # JSON 序列化配置
│   │   └── WebSocketConfig.java             # WebSocket 端点配置
│   │
│   ├── controller/                          # REST API 控制器
│   │   ├── ConfigController.java            # 配置管理 API
│   │   └── EmbeddingController.java         # 向量化/搜索 API
│   │
│   ├── websocket/                           # WebSocket 处理器
│   │   └── EmbeddingWebSocketHandler.java   # 双向实时通信
│   │
│   ├── service/                             # 业务逻辑
│   │   ├── ConfigService.java               # 配置管理服务
│   │   ├── EmbeddingService.java            # 向量化服务
│   │   ├── RerankService.java               # 重排序服务
│   │   └── VectorStorageService.java        # SQLite 向量存储
│   │
│   ├── listener/                            # 事件监听器
│   │   └── ConfigChangeBroadcaster.java     # 异步广播配置变更
│   │
│   ├── event/                               # 事件定义
│   │   └── ConfigChangedEvent.java          # 配置变更事件
│   │
│   └── model/                               # 数据模型
│       ├── Message.java                     # 聊天消息记录
│       ├── EmbeddingRequest.java            # 向量化请求
│       ├── EmbeddingResult.java             # 向量化结果
│       ├── SearchRequest.java               # 搜索请求
│       └── SearchResult.java                # 搜索结果
│
├── src/main/resources/
│   └── application.yml                      # 应用配置文件
│
├── pom.xml                                   # Maven 构建配置
└── README.md                                 # 使用文档
```

---

## 核心组件说明

### 1. 启动入口

#### `EmbeddingDemoApplication.java`
- **作用**: Spring Boot 主启动类
- **关键注解**:
  - `@SpringBootApplication`: 自动配置 + 组件扫描
  - `@RegisterReflectionForBinding`: 注册 5 个 record 类用于 GraalVM 反射（WebSocket JSON 序列化需要）
- **启动方式**: Virtual Threads (虚拟线程)

---

### 2. 配置层 (config/)

#### `EmbeddingConfig.java`
- **作用**: 核心配置类，管理 embedding 和 rerank 提供商配置
- **关键类**:
  - `ProviderConfig`: 统一的提供商配置（baseUrl, model, apiKey）
  - `SlidingWindowConfig`: 滑动窗口配置（windowSize, overlapRatio, enableNearby, nearbyCount）
  - `ChunkConfig`: 长消息分块配置（enabled, maxLength）
  - `StorageConfig`: SQLite 存储路径配置
- **配置项**:
  ```yaml
  embedding:
    provider:        # embedding 提供商
      baseUrl: http://localhost:11434/v1
      model: bge-m3:latest
      apiKey: ""
    rerank:          # rerank 提供商
      baseUrl: http://localhost:8080
      model: BAAI/bge-reranker-base
      apiKey: ""
    chunk:           # 长消息分块
      enabled: true
      max-length: 512
    sliding-window: {...}
    storage: {...}
  ```

#### `AsyncConfig.java`
- **作用**: 启用 Spring 异步事件支持
- **关键注解**: `@EnableAsync`
- **配合组件**: `ConfigChangeBroadcaster` 中的 `@Async @EventListener`
- **解决问题**: 让配置变更广播在独立线程执行，避免 WebSocket 并发发送冲突

#### `HttpClientConfig.java`
- **作用**: 提供 `RestTemplate` Bean
- **为什么需要**: GraalVM AOT 编译需要 Spring 管理的 Bean，不能直接 `new RestTemplate()`
- **使用者**: `EmbeddingService`, `RerankService`

#### `JacksonConfig.java`
- **作用**: 配置 JSON 序列化
- **关键配置**:
  - `INDENT_OUTPUT`: 美化输出
  - `WRITE_DATES_AS_TIMESTAMPS`: 时间戳格式
  - 支持 Java 8 时间类型、record 类型

#### `WebSocketConfig.java`
- **作用**: 注册 WebSocket 端点
- **端点**: `/ws/embedding` → `EmbeddingWebSocketHandler`

---

### 3. 控制器层 (controller/)

#### `EmbeddingController.java`
- **作用**: 提供 REST API
- **端点**:
  - `POST /api/v1/embed`: 批量向量化消息
  - `POST /api/v1/embed/text`: 单文本向量化
  - `POST /api/v1/search`: 向量搜索（支持 rerank + nearby）
  - `DELETE /api/v1/chat/{chatId}`: 删除会话数据
- **搜索流程**:
  ```
  if (useRerank):
      search(topK*2) → rerank(topK) → addNearby()
  else if (nearbyCount > 0):
      searchWithNearby(topK, nearbyCount)
  else:
      search(topK)
  ```

#### `ConfigController.java`
- **作用**: 运行时配置管理 API
- **端点**:
  - `GET /api/v1/config`: 获取当前配置
  - `POST /api/v1/config`: 更新配置（动态生效，触发 `ConfigChangedEvent`）
- **支持配置项**: provider.*, rerank.*, chunk.enabled, chunk.maxLength, slidingWindow.*, storage.*

---

### 4. WebSocket 层 (websocket/)

#### `EmbeddingWebSocketHandler.java`
- **作用**: 双向实时通信，支持所有 REST API 功能
- **关键特性**:
  - 使用 `ConcurrentWebSocketSessionDecorator` 包装 session，保证线程安全
  - 异步接收配置变更事件（通过 `ConfigChangeBroadcaster`）
- **支持操作**:
  - `embed`: 向量化
  - `search`: 搜索（同 REST API 的 search 流程）
  - `delete`: 删除会话
- **消息格式**:
  ```json
  // 请求
  {"action": "search", "chatId": "test", "query": "hello", "topK": 5, "useRerank": true, "nearbyCount": 2}

  // 响应
  {"type": "result", "action": "search", "data": [...]}

  // 配置变更广播
  {"type": "config_changed", "changedFields": ["provider.model"], "config": {...}}
  ```

---

### 5. 服务层 (service/)

#### `EmbeddingService.java`
- **作用**: 调用 embedding 提供商 API 生成向量
- **核心机制 — 长消息分块（Chunking）**:
  - `chunkMessages()`: 将消息列表转为 chunk 列表
    - 短消息（≤ maxLength）：整条存为一个 chunk
    - 长消息：按 `\n\n` → `\n` → 硬切三级分段，每段一个 chunk
    - assistant 回复的 chunk 自动附带最近 user 问题作为上下文锚点（截取前 200 字）
  - `splitIntoChunks()`: 段落切分 — 合并小段落，拆分超长段落
  - `Chunk` record: 内部数据结构（index, originalMessageIndex, id, content, role）
- **向量化模式**（均在 chunk 级别操作）:
  - `embedIndividually()`: 逐 chunk 向量化
  - `embedWithSlidingWindow()`: 滑动窗口在 chunks 上滑动
- **chunk.enabled=false 时**: 退化为每条消息一个 chunk，行为等同改动前
- **API 格式**: OpenAI-compatible `/embeddings` 端点（Ollama 使用 `/v1` 前缀）
- **请求示例**:
  ```json
  POST http://localhost:11434/v1/embeddings
  {"model": "bge-m3:latest", "input": "hello", "encoding_format": "float"}
  ```

#### `RerankService.java`
- **作用**: 调用 rerank 提供商 API 重排序搜索结果
- **兼容性**: 同时支持 Cohere/SiliconFlow 和 HuggingFace TEI 格式
- **请求格式**:
  ```json
  {
    "model": "BAAI/bge-reranker-base",
    "query": "search query",
    "documents": ["text1", "text2"],
    "texts": ["text1", "text2"],     // TEI 兼容
    "top_n": 5,
    "return_documents": false
  }
  ```
- **响应解析**:
  - 数组格式（TEI）: `[{index, score}, ...]`
  - 对象格式（Cohere/SiliconFlow）: `{results: [{index, relevance_score}, ...]}`

#### `VectorStorageService.java`
- **作用**: SQLite 向量存储和检索
- **核心功能**:
  - `saveEmbeddings()`: 批量存储向量
  - `search()`: 余弦相似度搜索
  - `searchWithNearby()`: 搜索 + 附近消息
  - `addNearby()`: 为已有结果添加附近消息（rerank 后调用）
  - `fetchByIndices()`: 按索引批量查询
- **数据结构**:
  ```sql
  CREATE TABLE embeddings (
    chat_id TEXT,
    message_index INTEGER,
    content TEXT,
    embedding BLOB,
    context_range TEXT,  -- 滑动窗口范围 "1-3"
    PRIMARY KEY (chat_id, message_index)
  )
  ```

#### `ConfigService.java`
- **作用**: 配置管理和变更通知
- **核心方法**:
  - `applyConfigUpdate()`: 应用配置更新，发布 `ConfigChangedEvent`
  - `detectDimension()`: 自动检测 embedding 维度
  - `getCurrentConfig()`: 获取配置快照
- **并发控制**: `synchronized` 保证配置更新的原子性

---

### 6. 事件层 (event/ + listener/)

#### `ConfigChangedEvent.java`
- **作用**: 配置变更事件定义
- **字段**:
  - `changedFields`: 变更的配置 key 列表
  - `configSnapshot`: 变更后的完整配置快照

#### `ConfigChangeBroadcaster.java`
- **作用**: 异步监听配置变更事件，广播给所有 WebSocket 客户端
- **关键注解**: `@Async @EventListener`
- **流程**: `ConfigService` 发布事件 → 独立线程接收 → `EmbeddingWebSocketHandler.broadcast()`

---

### 7. 数据模型层 (model/)

所有模型使用 Java 17+ `record` 类型（不可变、自动生成 getter/equals/hashCode）

#### `Message.java`
- **字段**: `messageIndex`, `content`
- **用途**: 原始聊天消息

#### `EmbeddingRequest.java`
- **字段**: `chatId`, `messages`, `useSlidingWindow`, `windowSize`
- **用途**: POST /api/v1/embed 请求体

#### `EmbeddingResult.java`
- **字段**: `messageIndex`, `content`, `embedding`, `contextRange`
- **用途**: 向量化结果

#### `SearchRequest.java`
- **字段**: `chatId`, `query`, `topK`, `useRerank`, `nearbyCount`
- **用途**: POST /api/v1/search 请求体

#### `SearchResult.java`
- **字段**: `messageIndex`, `content`, `score`, `isMatch`, `contextRange`
- **用途**: 搜索结果，`isMatch=false` 表示附近消息

---

## 核心流程

### 1. 向量化流程

```
用户提交消息 → EmbeddingController/WebSocketHandler
                ↓
        chunkMessages(): 消息分块
          短消息 → 整条存为一个 chunk
          长消息 → 按段落切分，附带上下文锚点
                ↓
        选择模式：逐 chunk / 滑动窗口（在 chunks 上滑动）
                ↓
        EmbeddingService 调用提供商 API
                ↓
        VectorStorageService 存储到 SQLite
```

### 2. 搜索流程（修复后）

```
用户提交查询 → EmbeddingController/WebSocketHandler
                ↓
        useRerank=true？
                ↓ 是
        search(topK*2, 过采样)
                ↓
        rerank(topK, 精排)
                ↓
        nearbyCount > 0？ → addNearby(附近消息)
                ↓
        返回结果

        ↓ 否（useRerank=false）
        nearbyCount > 0？
                ↓ 是
        searchWithNearby(topK, nearbyCount)
                ↓ 否
        search(topK)
```

### 3. 配置更新流程

```
POST /api/v1/config → ConfigController
                ↓
        ConfigService.applyConfigUpdate()
                ↓
        发布 ConfigChangedEvent
                ↓
        ConfigChangeBroadcaster (@Async 独立线程)
                ↓
        EmbeddingWebSocketHandler.broadcast()
                ↓
        所有 WebSocket 客户端收到通知
```

---

## GraalVM Native Image 兼容性

### 已解决的问题

1. **Record 类反射**: `@RegisterReflectionForBinding` 注册 5 个 model 类
2. **RestTemplate**: 使用 Spring Bean 而非 `new RestTemplate()`
3. **SQLite JNI**: sqlite-jdbc 3.49.1.0 自带 native-image 元数据
4. **AOT 优化**: `pom.xml` 添加 `-Djava.awt.headless=true`

### 构建命令

```bash
# 使用 GraalVM 25 JDK 21+
mvn -Pnative native:compile
```

---

## 配置文件

### `application.yml`
- **默认配置**: 仅保留服务器端口、虚拟线程、分块、滑动窗口、存储路径
- **运行时配置**: embedding/rerank 提供商需通过 `/api/v1/config` 设置；chunk 参数也可运行时调整
- **设计原因**: 支持动态切换提供商，无需重启服务

---

## 设计亮点

1. **统一 OpenAI-compatible 格式**: Ollama/SiliconFlow/任意兼容提供商均使用相同接口
2. **动态配置**: 运行时切换 embedding/rerank 提供商，实时生效
3. **长消息分块**: 自动切分长回复，附带用户问题锚点，滑窗和 nearby 在 chunk 级别无缝运行
4. **正确的搜索流程**: rerank 在附近消息之前执行，避免上下文丢失
5. **异步事件广播**: 避免 WebSocket 并发发送冲突
6. **GraalVM 原生支持**: 启动时间 <1s，内存占用 <50MB
