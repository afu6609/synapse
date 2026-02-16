package com.chatst.embeddingdemo.service;

import com.chatst.embeddingdemo.config.EmbeddingConfig;
import com.chatst.embeddingdemo.model.GraphAssociation;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.stream.Stream;

@Service
public class MemoryGraphService {

    private static final Logger log = LoggerFactory.getLogger(MemoryGraphService.class);

    private final EmbeddingConfig config;
    private Path basePath;

    public MemoryGraphService(EmbeddingConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        String configuredPath = config.getStorage().getBasePath();
        if (configuredPath == null || configuredPath.isBlank()) {
            configuredPath = "./data/embedding-service";
        }
        basePath = Path.of(configuredPath);
    }

    private Connection getConnection(String chatId) throws SQLException, IOException {
        Path chatDir = basePath.resolve(chatId);
        Files.createDirectories(chatDir);
        Path dbPath = chatDir.resolve("metadata.db");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        initializeGraphTable(conn);
        return conn;
    }

    private void initializeGraphTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS memory_graph (
                            node_a TEXT NOT NULL,
                            node_b TEXT NOT NULL,
                            weight REAL NOT NULL DEFAULT 1.0,
                            co_activation_count INTEGER NOT NULL DEFAULT 1,
                            last_activated_at TEXT NOT NULL DEFAULT (datetime('now')),
                            PRIMARY KEY (node_a, node_b),
                            CHECK (node_a < node_b)
                        )
                    """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_mg_node_a ON memory_graph(node_a)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_mg_node_b ON memory_graph(node_b)");
        }
    }

    /**
     * 记录共激活：对所有 C(N,2) 对执行 UPSERT
     */
    public void recordCoActivation(String chatId, List<String> nodeIds) {
        if (nodeIds == null || nodeIds.size() < 2) {
            return;
        }

        try (Connection conn = getConnection(chatId)) {
            String upsertSql = """
                        INSERT INTO memory_graph (node_a, node_b, weight, co_activation_count, last_activated_at)
                        VALUES (?, ?, 1.0, 1, datetime('now'))
                        ON CONFLICT(node_a, node_b) DO UPDATE SET
                            weight = weight + 1.0,
                            co_activation_count = co_activation_count + 1,
                            last_activated_at = datetime('now')
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(upsertSql)) {
                for (int i = 0; i < nodeIds.size(); i++) {
                    for (int j = i + 1; j < nodeIds.size(); j++) {
                        String a = nodeIds.get(i);
                        String b = nodeIds.get(j);
                        // 保证 node_a < node_b
                        if (a.compareTo(b) > 0) {
                            String tmp = a;
                            a = b;
                            b = tmp;
                        }
                        stmt.setString(1, a);
                        stmt.setString(2, b);
                        stmt.executeUpdate();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to record co-activation for chat {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * 查询关联节点
     */
    public List<GraphAssociation> queryAssociations(String chatId, Set<String> seedIds,
            double threshold, int maxResults) {
        if (seedIds == null || seedIds.isEmpty()) {
            return Collections.emptyList();
        }

        try (Connection conn = getConnection(chatId)) {
            // 查找所有与 seedIds 相关联且 weight >= threshold 的节点
            String placeholders = seedIds.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("");
            String sql = """
                        SELECT CASE WHEN node_a IN (%s) THEN node_b ELSE node_a END AS associated_node,
                               MAX(weight) AS max_weight
                        FROM memory_graph
                        WHERE (node_a IN (%s) OR node_b IN (%s))
                          AND weight >= ?
                        GROUP BY associated_node
                        HAVING associated_node NOT IN (%s)
                        ORDER BY max_weight DESC
                        LIMIT ?
                    """.formatted(placeholders, placeholders, placeholders, placeholders);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int paramIdx = 1;
                // Groups 1-3: CASE IN, WHERE node_a IN, WHERE node_b IN
                for (int g = 0; g < 3; g++) {
                    for (String id : seedIds) {
                        stmt.setString(paramIdx++, id);
                    }
                }
                // weight >= ? (threshold comes between group 3 and 4 in SQL)
                stmt.setDouble(paramIdx++, threshold);
                // Group 4: HAVING NOT IN
                for (String id : seedIds) {
                    stmt.setString(paramIdx++, id);
                }
                // LIMIT ?
                stmt.setInt(paramIdx, maxResults);

                List<GraphAssociation> results = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(new GraphAssociation(
                                rs.getString("associated_node"),
                                rs.getDouble("max_weight")));
                    }
                }
                return results;
            }
        } catch (Exception e) {
            log.warn("Failed to query graph associations for chat {}: {}", chatId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 删除涉及某节点的所有边
     */
    public void removeNode(String chatId, String windowId) {
        try (Connection conn = getConnection(chatId)) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM memory_graph WHERE node_a = ? OR node_b = ?")) {
                stmt.setString(1, windowId);
                stmt.setString(2, windowId);
                int deleted = stmt.executeUpdate();
                if (deleted > 0) {
                    log.info("Removed {} graph edges for node {} in chat {}", deleted, windowId, chatId);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to remove graph node {} for chat {}: {}", windowId, chatId, e.getMessage());
        }
    }

    /**
     * 衰减并剪枝单个 chatId
     */
    public void decayAndPrune(String chatId, double decayFactor, double pruneThreshold) {
        try (Connection conn = getConnection(chatId)) {
            // 衰减权重
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE memory_graph SET weight = weight * ?")) {
                stmt.setDouble(1, decayFactor);
                stmt.executeUpdate();
            }

            // 剪枝低权重边
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM memory_graph WHERE weight < ?")) {
                stmt.setDouble(1, pruneThreshold);
                int pruned = stmt.executeUpdate();
                if (pruned > 0) {
                    log.info("Pruned {} low-weight edges for chat {}", pruned, chatId);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to decay/prune graph for chat {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * 遍历所有 chatId 目录执行衰减
     */
    public void decayAll(double decayFactor, double pruneThreshold) {
        try (Stream<Path> dirs = Files.list(basePath)) {
            dirs.filter(Files::isDirectory)
                    .forEach(dir -> {
                        String chatId = dir.getFileName().toString();
                        Path dbPath = dir.resolve("metadata.db");
                        if (Files.exists(dbPath)) {
                            decayAndPrune(chatId, decayFactor, pruneThreshold);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to iterate chat directories for graph decay: {}", e.getMessage());
        }
    }
}
