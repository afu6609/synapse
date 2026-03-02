package com.synapse.embedding.service;

import com.synapse.embedding.config.EmbeddingConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 配置持久化服务 — 使用 SQLite 存储运行时配置。
 * 配置文件位于 storage.basePath 下的 config.db。
 */
@Service
public class ConfigStorageService {

    private static final Logger log = LoggerFactory.getLogger(ConfigStorageService.class);

    private final EmbeddingConfig config;
    private Path dbPath;

    public ConfigStorageService(EmbeddingConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() throws IOException {
        String basePath = config.getStorage().getBasePath();
        if (basePath == null || basePath.isBlank()) {
            basePath = "./data/embedding-service";
        }
        Path dir = Path.of(basePath);
        Files.createDirectories(dir);
        dbPath = dir.resolve("config.db");

        try (Connection conn = getConnection()) {
            initializeDatabase(conn);
        } catch (SQLException e) {
            log.error("Failed to initialize config database", e);
            throw new RuntimeException("Config database initialization failed", e);
        }

        log.info("Config storage initialized at: {}", dbPath);
    }

    /**
     * 加载所有已持久化的配置。
     */
    public Map<String, String> loadAll() {
        Map<String, String> result = new LinkedHashMap<>();
        String sql = "SELECT key, value FROM config ORDER BY key";

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.put(rs.getString("key"), rs.getString("value"));
            }
        } catch (SQLException e) {
            log.error("Failed to load persisted config", e);
        }

        return result;
    }

    /**
     * 保存单条配置。
     */
    public void save(String key, String value) {
        String sql = "INSERT OR REPLACE INTO config (key, value) VALUES (?, ?)";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to save config key: {}", key, e);
        }
    }

    /**
     * 批量保存配置。
     */
    public void saveAll(Map<String, Object> configs) {
        String sql = "INSERT OR REPLACE INTO config (key, value) VALUES (?, ?)";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (Map.Entry<String, Object> entry : configs.entrySet()) {
                pstmt.setString(1, entry.getKey());
                pstmt.setString(2, entry.getValue() != null ? entry.getValue().toString() : null);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
            log.debug("Persisted {} config entries", configs.size());
        } catch (SQLException e) {
            log.error("Failed to batch save config", e);
        }
    }

    /**
     * 删除单条配置。
     */
    public void deleteKey(String key) {
        String sql = "DELETE FROM config WHERE key = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to delete config key: {}", key, e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
    }

    private void initializeDatabase(Connection conn) throws SQLException {
        String createTableSql = """
                    CREATE TABLE IF NOT EXISTS config (
                        key   TEXT PRIMARY KEY,
                        value TEXT
                    )
                """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
        }
    }
}
