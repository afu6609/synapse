# Synapse

English | [中文](README.md)

A **vector embedding and memory association service** built on Spring Boot 4.0.2. Compatible with all OpenAI-compatible Embedding/Rerank API providers. Also supports local ONNX embedding models. All configurations can be modified at runtime. Provides both REST API and WebSocket interfaces. Supports GraalVM Native Image compilation.

---

## ✨ Key Features

- **Unified API Access** — Compatible with Ollama, SiliconFlow, OpenAI, Jina, and all OpenAI-compatible Embedding/Rerank APIs
- **Local ONNX Models** — Built-in BGE-Small-ZH-v15, generate vectors without any external service
- **Runtime Configuration** — All provider settings configured via API at runtime, no restart required
- **Config Persistence** — Runtime configurations automatically saved to SQLite, restored on restart
- **Automatic Message Chunking** — Long responses automatically split by paragraph with context anchors
- **Memory Association Graph** — Passive learning system based on "co-activation" principle, automatically builds associations during search and activates related memories
- **Vector Search + Rerank** — Cosine similarity search + reranking + nearby message retrieval + graph association activation
- **Precise Deletion** — Delete individual embeddings by windowId, automatically cleans up associated graph edges
- **Dual Protocol** — REST API and WebSocket with full feature parity
- **Web Admin UI** — Built-in visual management panel for config, graph visualization, and search testing
- **GraalVM Native Image** — Native image compilation support, <1s startup time, <50MB memory footprint

---

## 🚀 Quick Start

### Requirements

- JDK 21+
- Maven 3.9+
- (Optional) GraalVM 25 (for Native Image)

### Start the Service

```bash
mvn spring-boot:run
```

Default port: `23456`. No pre-configuration needed — providers can be configured via API or Admin UI after startup.

### 🖥️ Web Admin UI

After startup, open:

```
http://localhost:23456/admin/
```

The built-in Web Admin UI provides:
- **Configuration** — Visual setup for Embedding/Rerank providers, one-click save with automatic dimension detection
- **Memory Graph Visualization** — Rendered with vis.js, click to weaken specific association edges
- **Search Testing** — Live search testing with graph association and Rerank options
- **Embedding Test** — Single text or batch message embedding test

### Configure Embedding Provider

**Ollama (Local):**

```bash
curl -X PATCH http://localhost:23456/api/v1/config \
  -H "Content-Type: application/json" \
  -d '{
    "provider.baseUrl": "http://localhost:11434/v1",
    "provider.model": "qwen3-embedding-4b"
  }'
```

**Cloud API (SiliconFlow, etc.):**

```bash
curl -X PATCH http://localhost:23456/api/v1/config \
  -H "Content-Type: application/json" \
  -d '{
    "provider.baseUrl": "https://api.siliconflow.cn/v1",
    "provider.model": "Qwen/Qwen3-Embedding-8B",
    "provider.apiKey": "sk-your-api-key"
  }'
```

**Local ONNX Model (No External Service Required):**

```bash
curl -X PATCH http://localhost:23456/api/v1/config \
  -H "Content-Type: application/json" \
  -d '{
    "provider.type": "local",
    "provider.model": "bge-small-zh-v15"
  }'
```

### Configure Rerank (Optional)

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

Base URL: `http://localhost:23456/api/v1`

### Health Check

```
GET /api/v1/health
→ { "status": "ok", "service": "embedding-service" }
```

### Embed Messages

```
POST /api/v1/embed
```

```json
{
  "chatId": "chat-001",
  "messages": [
    { "id": "msg1", "role": "user", "content": "Hello", "timestamp": 1700000000 },
    { "id": "msg2", "role": "assistant", "content": "Hi! How can I help?", "timestamp": 1700000001 }
  ],
  "useSlidingWindow": true,
  "windowSize": 2
}
```

### Embed Single Text

```
POST /api/v1/embed/text
```

```json
{ "text": "Some text to embed" }
```

### Search Similar Content

```
POST /api/v1/search
```

```json
{
  "chatId": "chat-001",
  "query": "What is a vector database",
  "topK": 5,
  "useRerank": true,
  "nearbyCount": 2,
  "useGraph": true
}
```

| Response Field | Description |
|----------------|-------------|
| `matchType: "vector"` | Direct vector match |
| `matchType: "nearby"` | Nearby context |
| `matchType: "graph"` | Graph association activation |

> ⚠️ **Note**: After switching embedding models, if stored vectors have a different dimension than the new model, search returns `500` with a "dimension mismatch" error. Clear old vectors (`DELETE /api/v1/chat/{chatId}`) and re-embed.

### Delete Operations

```
DELETE /api/v1/chat/{chatId}/embedding/{windowId}  # Delete single embedding
DELETE /api/v1/chat/{chatId}                        # Delete entire chat data
```

### Configuration Management

```
GET   /api/v1/config                    # Get current configuration
PATCH /api/v1/config                    # Update configuration (auto-persisted; provider changes auto-detect dimension)
POST  /api/v1/config/detect-dimension   # Manual dimension detection
```

### Graph Management

```
GET  /api/v1/chat/{chatId}/graph          # List graph edges
POST /api/v1/chat/{chatId}/graph/weaken   # Weaken a specific edge
POST /api/v1/chat/{chatId}/graph/decay    # Manually trigger decay
```

**Weaken edge request body:**

```json
{
  "nodeA": "msg1-msg2",
  "nodeB": "msg3-msg4",
  "amount": 0.5
}
```

- `amount` is optional, defaults to `1.0`; edge is removed when weight drops to ≤ 0

---

## 🔌 WebSocket API

### Connect

```
ws://localhost:23456/ws/embedding
```

### Supported Actions

| Action | Description |
|--------|-------------|
| `embed` | Embed messages |
| `search` | Search similar content (supports `useGraph`) |
| `delete` | Delete data (supports `windowId` for single deletion) |
| `config` | Configuration management (get / update / detect-dimension) |
| `graph` | Graph management (get / weaken / decay) |
| `ping` | Heartbeat |

Configuration updates are automatically broadcast as `config_changed` events to all connected clients.

---

## ⚙️ Runtime Configuration

All settings are configured dynamically via API — no config file changes or restarts needed. Configuration changes are automatically persisted to SQLite (`config.db`) and restored on restart.

| Key | Description |
|-----|-------------|
| `provider.type` | `api` (default) or `local` |
| `provider.baseUrl` | Embedding API endpoint |
| `provider.model` | Model name |
| `provider.apiKey` | API key (optional) |
| `rerank.baseUrl` | Rerank API endpoint |
| `rerank.model` | Rerank model name |
| `rerank.apiKey` | Rerank API key (optional) |
| `chunk.enabled` | Message chunking toggle (default: `true`) |
| `chunk.maxLength` | Chunk threshold (default: `512`) |
| `graph.enabled` | Memory graph toggle (default: `true`) |
| `graph.decayFactor` | Decay factor (default: `0.95`) |
| `graph.pruneThreshold` | Pruning threshold (default: `0.01`) |
| `graph.queryThreshold` | Query threshold (default: `0.5`) |
| `graph.maxGraphResults` | Max graph results (default: `5`) |

---

## 🧠 Memory Association Graph

A passive learning system based on the "co-activation" principle:

1. **Passive Learning** — During search, all directly matched results are automatically recorded as co-activation pairs
2. **Association Activation** — Search queries associated memories of directly matched nodes and appends them to results
3. **Automatic Decay** — Scheduled task (default: daily at 3 AM) multiplies all edge weights by decay factor and prunes low-weight edges
4. **Manual Decay** — Trigger decay for a single chat via API (`POST .../graph/decay`)
5. **Precise Weakening** — Weaken a specific edge between two nodes via API (`POST .../graph/weaken`); subtraction-based, edge removed when weight ≤ 0
6. **Graph Inspection** — View all edges and weights via Admin UI or API (`GET .../graph`)
7. **Node Cleanup** — Deleting an embedding automatically removes all its associated graph edges

---

## 🏗️ GraalVM Native Image

### Standard Build

```bash
mvn -Pnative native:compile -DskipTests
```

### ⚠️ Using Local ONNX Model (bge-small-zh-v15)

If `provider.type=local` is enabled, you must run the JAR once on the target machine first to let DJL download the native libraries:

```bash
# Step 1: Build JAR and run once to download DJL native libs to ~/.djl.ai/
mvn package -DskipTests
java -jar target/synapse-0.1.0.jar
# Wait for "Started SynapseApplication", then Ctrl+C to stop

# Step 2: Build Native Image (antrun-plugin auto-copies libs from ~/.djl.ai/ to target/)
mvn -Pnative native:compile -DskipTests
```

> Only required once on each **new machine**. Subsequent `native:compile` runs can skip Step 1.
> Not required when using only external API mode.

### Required Files for Distribution

| Platform | Required Files |
|----------|----------------|
| **Windows** (local model) | `synapse.exe` + `tokenizers.dll` + `libwinpthread-1.dll` + `libgcc_s_seh-1.dll` + `libstdc++-6.dll` |
| **Windows** (API only) | `synapse.exe` |
| **Linux** (local model) | `synapse` + `libtokenizers.so` |
| **Linux** (API only) | `synapse` |

The `data/` directory (SQLite config DB + vector files) is created automatically at runtime.

### Generate Reflection Config (Troubleshooting)

```bash
java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image \
  -jar target/synapse-0.1.0.jar
```

---

## 📄 License

This project is licensed under the [Cooperative Non-Commercial License (CNC-1.0)](LICENSE).

- ✅ Free for personal, educational, and research use
- ✅ Free to modify and distribute
- ❌ Commercial use prohibited
- 📋 Derivative works must maintain the same license

See the [LICENSE](LICENSE) file for details.
