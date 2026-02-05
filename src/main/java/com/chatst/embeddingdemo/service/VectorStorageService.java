package com.chatst.embeddingdemo.service;

import com.chatst.embeddingdemo.config.EmbeddingConfig;
import com.chatst.embeddingdemo.model.EmbeddingResult;
import com.chatst.embeddingdemo.model.SearchResult;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

@Service
public class VectorStorageService {

    private static final Logger log = LoggerFactory.getLogger(VectorStorageService.class);

    private final EmbeddingConfig config;
    private final EmbeddingService embeddingService;
    private Path basePath;

    public VectorStorageService(EmbeddingConfig config, EmbeddingService embeddingService) {
        this.config = config;
        this.embeddingService = embeddingService;
    }

    @PostConstruct
    public void init() throws IOException {
        basePath = Path.of(config.getStorage().getBasePath());
        Files.createDirectories(basePath);
        log.info("Vector storage initialized at: {}", basePath);
    }

    /**
     * 保存向量化结果
     */
    public void saveEmbeddings(String chatId, List<EmbeddingResult> results) throws Exception {
        Path chatDir = basePath.resolve(chatId);
        Files.createDirectories(chatDir);

        Path dbPath = chatDir.resolve("metadata.db");

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            initializeDatabase(conn);

            String insertSql = """
                INSERT OR REPLACE INTO embeddings (window_id, content, message_ids, vector_file)
                VALUES (?, ?, ?, ?)
            """;

            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                for (EmbeddingResult result : results) {
                    String vectorFile = result.windowId() + config.getStorage().getVectorFileSuffix();

                    // 保存向量到二进制文件
                    saveVectorToFile(chatDir.resolve(vectorFile), result.vector());

                    stmt.setString(1, result.windowId());
                    stmt.setString(2, result.content());
                    stmt.setString(3, String.join(",", result.messageIds()));
                    stmt.setString(4, vectorFile);
                    stmt.executeUpdate();
                }
            }
        }

        log.info("Saved {} embeddings for chat: {}", results.size(), chatId);
    }

    /**
     * 搜索相似向量
     */
    public List<SearchResult> search(String chatId, String query, int topK) throws Exception {
        Path chatDir = basePath.resolve(chatId);
        Path dbPath = chatDir.resolve("metadata.db");

        if (!Files.exists(dbPath)) {
            return Collections.emptyList();
        }

        // 获取查询文本的向量
        List<Float> queryVector = embeddingService.getEmbedding(query);

        List<SearchResult> results = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            String selectSql = "SELECT window_id, content, message_ids, vector_file FROM embeddings";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(selectSql)) {

                while (rs.next()) {
                    String windowId = rs.getString("window_id");
                    String content = rs.getString("content");
                    String messageIds = rs.getString("message_ids");
                    String vectorFile = rs.getString("vector_file");

                    List<Float> storedVector = loadVectorFromFile(chatDir.resolve(vectorFile));
                    double similarity = cosineSimilarity(queryVector, storedVector);

                    results.add(new SearchResult(
                            windowId,
                            content,
                            similarity,
                            Arrays.asList(messageIds.split(","))
                    ));
                }
            }
        }

        // 按相似度排序，取topK
        results.sort((a, b) -> Double.compare(b.score(), a.score()));
        return results.subList(0, Math.min(topK, results.size()));
    }

    /**
     * 删除聊天的所有向量数据
     */
    public void deleteChat(String chatId) throws IOException {
        Path chatDir = basePath.resolve(chatId);
        if (Files.exists(chatDir)) {
            try (var paths = Files.walk(chatDir)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("Failed to delete: {}", path, e);
                            }
                        });
            }
            log.info("Deleted all data for chat: {}", chatId);
        }
    }

    private void initializeDatabase(Connection conn) throws SQLException {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS embeddings (
                window_id TEXT PRIMARY KEY,
                content TEXT NOT NULL,
                message_ids TEXT NOT NULL,
                vector_file TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
        }
    }

    private void saveVectorToFile(Path path, List<Float> vector) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(path)))) {
            dos.writeInt(vector.size());
            for (Float f : vector) {
                dos.writeFloat(f);
            }
        }
    }

    private List<Float> loadVectorFromFile(Path path) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(path)))) {
            int size = dis.readInt();
            List<Float> vector = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                vector.add(dis.readFloat());
            }
            return vector;
        }
    }

    /**
     * 余弦相似度计算
     */
    private double cosineSimilarity(List<Float> v1, List<Float> v2) {
        if (v1.size() != v2.size()) {
            throw new IllegalArgumentException("Vectors must have same dimension");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < v1.size(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            norm1 += v1.get(i) * v1.get(i);
            norm2 += v2.get(i) * v2.get(i);
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
