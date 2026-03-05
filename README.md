# Synapse

[English](README_EN.md) | 中文

基于 Spring Boot 4.0.2 的**向量嵌入与记忆关联服务**。支持所有 OpenAI 兼容 API 的 Embedding/Rerank 提供商，也支持本地 ONNX 嵌入模型。所有配置均可运行时动态修改。提供 REST API 和 WebSocket 双协议接口。支持 GraalVM Native Image 编译。

---

## ✨ 核心特性

- **统一 API 接入** — 兼容 Ollama、SiliconFlow、OpenAI、Jina 等所有 OpenAI 兼容的 Embedding/Rerank API
- **本地 ONNX 模型** — 内置 BGE-Small-ZH-v15，无需外部服务即可生成向量
- **运行时动态配置** — 所有提供商配置均通过 API 在运行时设置，无需重启
- **配置持久化** — 运行时配置自动保存到 SQLite，重启后自动恢复，无需重复配置
- **长消息自动分块** — 超长回复自动按段落切分，附带上下文锚点
- **记忆关联拓扑图** — 基于"共激活"原理的被动学习系统，搜索时自动建立关联，激活相关记忆
- **向量搜索 + Rerank** — 余弦相似度搜索 + 重排序 + 附近消息检索 + 图关联激活
- **单条精确删除** — 按 windowId 精确删除嵌入，自动清理图中关联边
- **双协议支持** — REST API 和 WebSocket 功能完全对等
- **Web 管理界面** — 内置可视化管理面板，支持配置管理、图关联可视化、搜索测试
- **GraalVM Native Image** — 支持原生镜像编译，启动时间 <1s，内存占用 <50MB

---

## 🚀 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- （可选）GraalVM 25（打包 Native Image）

### 启动服务

```bash
mvn spring-boot:run
```

服务默认端口 `23456`，启动后无需预先配置 provider，可通过 API 或管理界面动态配置。

### 🖥️ Web 管理界面

启动后访问：

```
http://localhost:23456/admin/
```

内置 Web 管理界面提供：
- **配置管理** — 图形化设置 Embedding/Rerank 提供商、一键保存并自动检测维度
- **记忆图可视化** — 使用 vis.js 渲染图网络，支持点击削弱指定关联边
- **向量搜索测试** — 在线测试搜索效果，支持图关联和 Rerank 选项
- **嵌入测试** — 单条文本或多条消息批量嵌入测试

### 配置 Embedding 提供商

**Ollama（本地）：**

```bash
curl -X PATCH http://localhost:23456/api/v1/config \
  -H "Content-Type: application/json" \
  -d '{
    "provider.baseUrl": "http://localhost:11434/v1",
    "provider.model": "qwen3-embedding-4b"
  }'
```

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

**本地 ONNX 模型（无需外部服务）：**

```bash
curl -X PATCH http://localhost:23456/api/v1/config \
  -H "Content-Type: application/json" \
  -d '{
    "provider.type": "local",
    "provider.model": "bge-small-zh-v15"
  }'
```

### 配置 Rerank（可选）

```bash
curl -X PATCH http://localhost:23456/api/v1/config \
  -H "Content-Type: application/json" \
  -d '{
    "rerank.baseUrl": "https://api.siliconflow.cn/v1",
    "rerank.model": "Qwen/Qwen3-Reranker-8B",
    "rerank.apiKey": "sk-your-api-key"
  }'
```

---

## 📡 REST API

基础路径: `http://localhost:23456/api/v1`

### 健康检查

```
GET /api/v1/health
→ { "status": "ok", "service": "embedding-service" }
```

### 向量化消息

```
POST /api/v1/embed
```

```json
{
  "chatId": "chat-001",
  "messages": [
    { "id": "msg1", "role": "user", "content": "你好", "timestamp": 1700000000 },
    { "id": "msg2", "role": "assistant", "content": "你好！有什么可以帮你的？", "timestamp": 1700000001 }
  ],
  "useSlidingWindow": true,
  "windowSize": 2
}
```

### 单条文本向量化

```
POST /api/v1/embed/text
```

```json
{ "text": "这是一段测试文本" }
```

### 搜索相似内容

```
POST /api/v1/search
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

| 响应字段 | 说明 |
|----------|------|
| `matchType: "vector"` | 向量直接命中 |
| `matchType: "nearby"` | 附近上下文 |
| `matchType: "graph"` | 图关联激活 |

> ⚠️ **注意**：切换嵌入模型后新旧向量维度不同时，搜索接口返回 `500` 并提示"维度不一致"。需清空旧向量（`DELETE /api/v1/chat/{chatId}`）再重新嵌入。

### 删除操作

```
DELETE /api/v1/chat/{chatId}/embedding/{windowId}  # 删除单条嵌入
DELETE /api/v1/chat/{chatId}                        # 删除整个会话数据
```

### 配置管理

```
GET   /api/v1/config                    # 获取当前配置
PATCH /api/v1/config                    # 更新配置（自动持久化，provider 变更后自动检测维度）
POST  /api/v1/config/detect-dimension   # 手动触发维度检测
```

### 图关联管理

```
GET  /api/v1/chat/{chatId}/graph          # 查看图边列表
POST /api/v1/chat/{chatId}/graph/weaken   # 削弱指定边
POST /api/v1/chat/{chatId}/graph/decay    # 手动触发衰减
```

**削弱边请求体：**

```json
{
  "nodeA": "msg1-msg2",
  "nodeB": "msg3-msg4",
  "amount": 0.5
}
```

- `amount` 可选，默认 `1.0`；权重减至 ≤ 0 时自动删除该边

---

## 🔌 WebSocket API

### 连接

```
ws://localhost:23456/ws/embedding
```

### 支持的操作

| Action | 说明 |
|--------|------|
| `embed` | 向量化消息 |
| `search` | 搜索相似内容（支持 `useGraph`） |
| `delete` | 删除数据（支持 `windowId` 单条删除） |
| `config` | 配置管理（get / update / detect-dimension） |
| `graph` | 图关联管理（get / weaken / decay） |
| `ping` | 心跳 |

配置更新时自动广播 `config_changed` 事件至所有已连接客户端。

---

## ⚙️ 运行时配置

所有配置通过 API 动态设置，无需修改配置文件重启。配置变更自动持久化到 SQLite（`config.db`），重启后自动恢复。

| Key | 说明 |
|-----|------|
| `provider.type` | `api`（默认）或 `local` |
| `provider.baseUrl` | Embedding API 地址 |
| `provider.model` | 模型名称 |
| `provider.apiKey` | API 密钥（可选） |
| `rerank.baseUrl` | Rerank API 地址 |
| `rerank.model` | Rerank 模型名称 |
| `rerank.apiKey` | Rerank API 密钥（可选） |
| `chunk.enabled` | 长消息分块开关（默认 `true`） |
| `chunk.maxLength` | 分块阈值（默认 `512`） |
| `graph.enabled` | 记忆关联图开关（默认 `true`） |
| `graph.decayFactor` | 衰减因子（默认 `0.95`） |
| `graph.pruneThreshold` | 剪枝阈值（默认 `0.01`） |
| `graph.queryThreshold` | 查询阈值（默认 `0.5`） |
| `graph.maxGraphResults` | 图关联最大返回数（默认 `5`） |

---

## 🧠 记忆关联图

基于"共激活"原理的被动学习系统：

1. **被动学习** — 搜索时，所有向量直接命中的结果自动两两记录为共激活对
2. **关联激活** — 搜索时根据直接命中节点查询关联记忆，追加到结果中
3. **自动衰减** — 定时任务（默认每天凌晨 3 点）对所有边权重乘以衰减因子，剪枝低权重边
4. **手动衰减** — 通过 API 手动触发单个会话的衰减（`POST .../graph/decay`）
5. **精准削弱** — 通过 API 削弱指定两个节点之间的关联（`POST .../graph/weaken`），减法操作，归零则删边
6. **图状态查看** — 通过管理界面或 API 查看指定会话的所有图边和权重
7. **节点清理** — 删除嵌入时自动清理该节点的所有关联边

---

## 🏗️ GraalVM Native Image

### 普通构建

```bash
mvn -Pnative native:compile -DskipTests
```

### ⚠️ 使用本地 ONNX 模型时（bge-small-zh-v15）

如果使用 `provider.type=local`，打包前需在目标机器上**先跑一次 JVM 版本**，让 DJL 完成原生库的首次下载缓存：

```bash
# 第一步：打 JAR 并运行，触发 DJL 下载原生库到 ~/.djl.ai/
mvn package -DskipTests
java -jar target/synapse-0.1.0.jar
# 看到 "Started SynapseApplication" 后 Ctrl+C 停止

# 第二步：打 Native Image（构建时会自动从 ~/.djl.ai/ 复制原生库到 target/）
mvn -Pnative native:compile -DskipTests
```

> 每台**新机器**只需做一次，之后每次 `native:compile` 可直接执行。
> 仅使用外部 API 嵌入时无需此步骤。

### 发布所需文件

| 平台 | 必须文件 |
|------|----------|
| **Windows**（使用本地模型） | `synapse.exe` + `tokenizers.dll` + `libwinpthread-1.dll` + `libgcc_s_seh-1.dll` + `libstdc++-6.dll` |
| **Windows**（仅 API 模式） | `synapse.exe` |
| **Linux**（使用本地模型） | `synapse` + `libtokenizers.so` |
| **Linux**（仅 API 模式） | `synapse` |

运行时自动创建 `data/` 目录（SQLite 配置库 + 向量文件）。

### 生成反射配置（排查问题）

```bash
java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image \
  -jar target/synapse-0.1.0.jar
```

---

## 📄 许可证

本项目基于 [Cooperative Non-Commercial License (CNC-1.0)](LICENSE) 开源。

- ✅ 个人、教育、研究用途自由使用
- ✅ 自由修改和分发
- ❌ 禁止商业用途
- 📋 衍生作品须保持相同许可

详见 [LICENSE](LICENSE) 文件。
