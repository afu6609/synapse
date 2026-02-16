# 项目结构说明

## 概述

embeddingDemo 是一个基于 Spring Boot 4.0.2 的向量嵌入服务，支持：
- 运行时动态配置 embedding 和 rerank 提供商
- 长消息自动分块（Chunking）+ 上下文锚点
- 滑动窗口向量化（在 chunk 级别滑动）
- 向量搜索 + 重排序 + 附近消息检索
- **记忆关联拓扑图** — 搜索时自动记录"共激活"关系，频繁一起出现的记忆建立强边，后续搜索可激活关联记忆
- **单条嵌入删除** — 支持按 windowId 精确删除单条嵌入
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
│   │   ├── EmbeddingConfig.java             # 核心配置（embedding/rerank/graph）
│   │   ├── HttpClientConfig.java            # RestTemplate Bean 配置
│   │   ├── JacksonConfig.java               # JSON 序列化配置
│   │   ├── SchedulingConfig.java            # 启用 @Scheduled 定时任务
│   │   └── WebSocketConfig.java             # WebSocket 端点配置
│   │
│   ├── controller/                          # REST API 控制器
│   │   ├── ConfigController.java            # 配置管理 API
│   │   └── EmbeddingController.java         # 向量化/搜索/删除 API
│   │
│   ├── websocket/                           # WebSocket 处理器
│   │   └── EmbeddingWebSocketHandler.java   # 双向实时通信
│   │
│   ├── service/                             # 业务逻辑
│   │   ├── ConfigService.java               # 配置管理服务
│   │   ├── EmbeddingService.java            # 向量化服务
│   │   ├── MemoryGraphService.java          # 记忆关联图服务（共激活/查询/衰减）
│   │   ├── RerankService.java               # 重排序服务
│   │   └── VectorStorageService.java        # SQLite 向量存储
│   │
│   ├── task/                                # 定时任务
│   │   └── GraphDecayTask.java              # 图权重衰减 + 低权重边剪枝
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
│       ├── GraphAssociation.java            # 图关联结果（windowId + weight）
│       ├── SearchRequest.java               # 搜索请求（含 useGraph）
│       └── SearchResult.java                # 搜索结果（matchType 三态）
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
  - `@RegisterReflectionForBinding`: 注册 6 个 record 类用于 GraalVM 反射（含 GraphAssociation）
- **启动方式**: Virtual Threads (虚拟线程)

---

### 2. 配置层 (config/)

#### `EmbeddingConfig.java`
- **作用**: 核心配置类，管理 embedding、rerank 和图关联配置
- **关键类**:
  - `ProviderConfig`: 统一的提供商配置（type, baseUrl, model, apiKey），type 支持 `api`（外部HTTP）和 `local`（本地ONNX模型）
  - `SlidingWindowConfig`: 滑动窗口配置
  - `ChunkConfig`: 长消息分块配置（enabled, maxLength）
  - `StorageConfig`: SQLite 存储路径配置
  - `GraphConfig`: 记忆关联图配置（enabled, decayFactor, pruneThreshold, queryThreshold, maxGraphResults, decayCron）

#### `SchedulingConfig.java`
- **作用**: 启用 Spring `@Scheduled` 注解支持
- **配合组件**: `GraphDecayTask`

#### `AsyncConfig.java`
- **作用**: 启用 Spring 异步事件支持
- **配合组件**: `ConfigChangeBroadcaster`

#### `HttpClientConfig.java`
- **作用**: 提供 `RestTemplate` Bean
- **使用者**: `EmbeddingService`, `RerankService`

#### `JacksonConfig.java` / `WebSocketConfig.java`
- JSON 序列化配置 / WebSocket 端点注册

---

### 3. 控制器层 (controller/)

#### `EmbeddingController.java`
- **作用**: 提供 REST API
- **端点**:
  - `POST /api/v1/embed`: 批量向量化消息
  - `POST /api/v1/embed/text`: 单文本向量化
  - `POST /api/v1/search`: 向量搜索（支持 rerank + nearby + graph）
  - `DELETE /api/v1/chat/{chatId}/embedding/{windowId}`: 单条嵌入删除
  - `DELETE /api/v1/chat/{chatId}`: 删除整个会话数据
- **搜索流程**:
  ```
  search / rerank / nearby
      ↓
  graph.enabled && results 非空？
      → 被动记录共激活（仅 vector 命中）
      → useGraph？查询图关联 → 合并到结果
  ```

#### `ConfigController.java`
- **作用**: 运行时配置管理 API（含 graph.* 配置）

---

### 4. WebSocket 层 (websocket/)

#### `EmbeddingWebSocketHandler.java`
- **作用**: 双向实时通信，功能与 REST API 对等
- **支持操作**:
  - `embed`: 向量化
  - `search`: 搜索（支持 `useGraph` 字段）
  - `delete`: 删除（支持 `windowId` 字段做单条删除）
  - `config`: 配置管理
  - `ping`: 心跳

---

### 5. 服务层 (service/)

#### `EmbeddingService.java`
- **作用**: 调用 embedding 提供商 API 或本地 ONNX 模型生成向量
- **核心机制**: 长消息分块 + 滑动窗口

#### `RerankService.java`
- **作用**: 调用 rerank 提供商 API 重排序搜索结果
- **兼容**: Cohere/SiliconFlow/Jina/HuggingFace TEI

#### `VectorStorageService.java`
- **作用**: SQLite 向量存储和检索
- **核心功能**:
  - `saveEmbeddings()`: 批量存储向量
  - `search()`: 余弦相似度搜索（matchType="vector"）
  - `addNearby()`: 添加附近消息（matchType="nearby"）
  - `fetchByWindowIds()`: 按 windowId 集合查询（用于图关联，matchType="graph"）
  - `deleteEmbedding()`: 单条嵌入删除
  - `deleteChat()`: 整个会话删除

#### `MemoryGraphService.java`
- **作用**: 记忆关联图的存储、查询和维护
- **核心方法**:
  - `recordCoActivation(chatId, nodeIds)`: 对 C(N,2) 对执行 UPSERT，weight +1.0
  - `queryAssociations(chatId, seedIds, threshold, maxResults)`: 查询关联节点
  - `removeNode(chatId, windowId)`: 删除涉及某节点的所有边
  - `decayAndPrune(chatId, factor, threshold)`: 衰减权重 + 剪枝低权重边
  - `decayAll(factor, threshold)`: 遍历所有 chatId 执行衰减
- **数据表** (memory_graph，在同一个 per-chat metadata.db 中):
  ```sql
  PRIMARY KEY (node_a, node_b), CHECK (node_a < node_b)
  weight REAL, co_activation_count INTEGER, last_activated_at TEXT
  ```

#### `ConfigService.java`
- **作用**: 配置管理和变更通知，支持 graph.* 配置项

---

### 6. 定时任务层 (task/)

#### `GraphDecayTask.java`
- **作用**: 按 cron 表达式定时执行图权重衰减
- **默认**: 每天凌晨 3 点（`0 0 3 * * *`）
- **逻辑**: `weight *= decayFactor` → 删除 `weight < pruneThreshold` 的边

---

### 7. 事件层 (event/ + listener/)

#### `ConfigChangedEvent.java` + `ConfigChangeBroadcaster.java`
- 配置变更事件广播给所有 WebSocket 客户端

---

### 8. 数据模型层 (model/)

所有模型使用 Java 17+ `record` 类型

| 模型 | 字段 | 用途 |
|------|------|------|
| `Message` | id, role, content, timestamp | 原始聊天消息 |
| `EmbeddingRequest` | chatId, messages, useSlidingWindow, windowSize | 向量化请求 |
| `EmbeddingResult` | windowId, content, vector, messageIds, messageIndex | 向量化结果 |
| `SearchRequest` | chatId, query, topK, useRerank, nearbyCount, **useGraph** | 搜索请求 |
| `SearchResult` | windowId, content, score, messageIds, messageIndex, **matchType** | 搜索结果 |
| `GraphAssociation` | windowId, weight | 图关联查询结果 |

**matchType 三态**: `"vector"` = 向量直接命中，`"nearby"` = 附近上下文，`"graph"` = 图关联激活

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

### 2. 搜索流程

```
用户提交查询 → EmbeddingController/WebSocketHandler
                ↓
        ┌─ useRerank=true ─────────────────────────────┐
        │  search(topK*2, 过采样)                       │
        │      ↓                                       │
        │  rerank(topK, 精排)                           │
        │      ↓                                       │
        │  nearbyCount > 0？ → addNearby(附近消息)       │
        ├─ useRerank=false, nearbyCount > 0 ───────────┤
        │  searchWithNearby(topK, nearbyCount)          │
        ├─ 默认 ───────────────────────────────────────┤
        │  search(topK)                                 │
        └──────────────────────────────────────────────┘
                ↓
        ┌─ graph.enabled && results 非空 ──────────────┐
        │  1. 收集 matchType="vector" 的 windowId       │
        │  2. 被动记录共激活（≥2 个直接命中时 UPSERT）    │
        │  3. useGraph=true？                           │
        │     → queryAssociations() 查关联节点           │
        │     → 排除已存在的 windowId                    │
        │     → fetchByWindowIds() 加载内容              │
        │     → 合并到结果（matchType="graph"）          │
        └──────────────────────────────────────────────┘
                ↓
        返回结果
```

### 3. 单条删除流程

```
DELETE /chat/{chatId}/embedding/{windowId}
    ↓
storageService.deleteEmbedding()
  → 查 vector_file → 删 DB 记录 → 删 .vec 文件
    ↓
memoryGraphService.removeNode()
  → 删除图中涉及该节点的所有边
```

### 4. 图衰减流程

```
GraphDecayTask (cron: 0 0 3 * * *)
    ↓
遍历所有 chatId 目录
    ↓
对每个 chatId:
  UPDATE weight = weight * decayFactor
  DELETE WHERE weight < pruneThreshold
```

### 5. 配置更新流程

```
PATCH /api/v1/config → ConfigController
                ↓
        ConfigService.applyConfigUpdate()
        (支持 graph.enabled / graph.decayFactor / graph.pruneThreshold 等)
                ↓
        发布 ConfigChangedEvent → WebSocket 广播
```

---

## 存储结构

```
./data/embedding-service/
└── {chatId}/
    ├── metadata.db                    (SQLite 数据库)
    │   ├── embeddings 表               (向量元数据)
    │   └── memory_graph 表             (记忆关联图)
    ├── {windowId}.vec                 (二进制向量文件)
    └── ...
```

---

## GraalVM Native Image 兼容性

1. **Record 类反射**: `@RegisterReflectionForBinding` 注册 6 个 model 类（含 GraphAssociation）
2. **RestTemplate**: 使用 Spring Bean
3. **SQLite JNI**: sqlite-jdbc 自带 native-image 元数据
4. **构建**: `mvn -Pnative native:compile`

---

## 设计亮点

1. **统一 OpenAI-compatible 格式**: 也支持本地 ONNX 模型
2. **动态配置**: 运行时切换 embedding/rerank 提供商及图关联参数
3. **长消息分块**: 自动切分长回复，附带用户问题锚点
4. **记忆关联图**: 被动学习共激活关系 + 按需激活关联记忆 + 自动衰减遗忘
5. **单条精确删除**: 按 windowId 删除嵌入并自动清理图边
6. **正确的搜索流程**: rerank → nearby → graph，层层叠加
7. **GraalVM 原生支持**: 启动时间 <1s，内存占用 <50MB
