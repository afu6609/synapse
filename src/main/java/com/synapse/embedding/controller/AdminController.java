package com.synapse.embedding.controller;

import com.synapse.embedding.config.EmbeddingConfig;
import com.synapse.embedding.service.ConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final EmbeddingConfig config;
    private final ConfigService configService;

    public AdminController(EmbeddingConfig config, ConfigService configService) {
        this.config = config;
        this.configService = configService;
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkAuth(HttpServletRequest request) {
        boolean required = config.getAdmin().isAuthRequired();
        boolean authenticated = !required;
        if (required) {
            HttpSession session = request.getSession(false);
            authenticated = session != null
                    && Boolean.TRUE.equals(session.getAttribute("admin_authenticated"));
        }
        return ResponseEntity.ok(Map.of(
                "authenticated", authenticated,
                "required", required));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        if (!config.getAdmin().isAuthRequired()) {
            return ResponseEntity.ok(Map.of("success", true));
        }

        String password = body.getOrDefault("password", "");
        if (config.getAdmin().getPassword().equals(password)) {
            HttpSession session = request.getSession(true);
            session.setAttribute("admin_authenticated", true);
            return ResponseEntity.ok(Map.of("success", true));
        }
        return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "Invalid password"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/password")
    public ResponseEntity<Map<String, Object>> setPassword(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String newPassword = body.getOrDefault("newPassword", "");

        // If auth is currently required, verify the current password
        if (config.getAdmin().isAuthRequired()) {
            String currentPassword = body.getOrDefault("currentPassword", "");
            if (!config.getAdmin().getPassword().equals(currentPassword)) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "error", "Current password is incorrect"));
            }
        }

        // Apply and persist via ConfigService
        configService.applyConfigUpdate(Map.of("admin.password", newPassword));

        // If clearing password, no need to re-auth; if setting, ensure session is valid
        if (!newPassword.isBlank()) {
            HttpSession session = request.getSession(true);
            session.setAttribute("admin_authenticated", true);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "authRequired", !newPassword.isBlank()));
    }

    /**
     * 从 OpenAI 兼容的 API 获取可用模型列表。
     */
    @PostMapping("/models")
    public ResponseEntity<Map<String, Object>> fetchModels(@RequestBody Map<String, String> body) {
        String baseUrl = body.getOrDefault("baseUrl", "").replaceAll("/+$", "");
        String apiKey = body.getOrDefault("apiKey", "");
        String role = body.getOrDefault("role", "provider");

        // If no apiKey provided, fall back to current configured key
        if (apiKey.isBlank()) {
            apiKey = "rerank".equals(role)
                    ? config.getRerank().getApiKey()
                    : config.getProvider().getApiKey();
            if (apiKey == null)
                apiKey = "";
        }

        if (baseUrl.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "baseUrl is required"));
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/models"))
                    .timeout(Duration.ofSeconds(15))
                    .GET();

            if (!apiKey.isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<String> resp = client.send(reqBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                return ResponseEntity.ok(Map.of(
                        "models", List.of(),
                        "count", 0,
                        "error", "API returned HTTP " + resp.statusCode()));
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(resp.body());
            JsonNode data = root.has("data") ? root.get("data") : root;

            List<String> models = new ArrayList<>();
            if (data.isArray()) {
                for (JsonNode node : data) {
                    String id = node.has("id") ? node.get("id").asText() : null;
                    if (id != null && !id.isBlank()) {
                        models.add(id);
                    }
                }
            }
            Collections.sort(models);

            return ResponseEntity.ok(Map.of("models", models, "count", models.size()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Request failed"));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("status", "running");
        stats.put("provider", buildProviderInfo(config.getProvider(), "embedding"));
        stats.put("rerank", buildProviderInfo(config.getRerank(), "rerank"));
        stats.put("dimension", config.getDetectedDimension());

        // 扫描存储目录，收集各 chatId 的统计
        List<Map<String, Object>> chats = new ArrayList<>();
        String basePath = config.getStorage().getBasePath();
        if (basePath != null && !basePath.isBlank()) {
            Path base = Path.of(basePath);
            if (Files.isDirectory(base)) {
                try (Stream<Path> dirs = Files.list(base)) {
                    dirs.filter(Files::isDirectory)
                            .filter(p -> !p.getFileName().toString().startsWith("."))
                            .forEach(chatDir -> {
                                String chatId = chatDir.getFileName().toString();
                                // 不统计 config.db 所在的根目录
                                if (Files.exists(chatDir.resolve("metadata.db"))) {
                                    Map<String, Object> chatInfo = new LinkedHashMap<>();
                                    chatInfo.put("chatId", chatId);
                                    chatInfo.put("embeddingCount", countEmbeddings(chatDir));
                                    chatInfo.put("graphEdgeCount", countGraphEdges(chatDir));
                                    chats.add(chatInfo);
                                }
                            });
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        stats.put("chats", chats);
        stats.put("totalChats", chats.size());
        stats.put("totalEmbeddings", chats.stream()
                .mapToInt(c -> (int) c.getOrDefault("embeddingCount", 0)).sum());

        return ResponseEntity.ok(stats);
    }

    private Map<String, Object> buildProviderInfo(EmbeddingConfig.ProviderConfig p, String role) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("configured", p.isConfigured());
        info.put("type", p.getType());
        info.put("model", p.getModel());
        info.put("baseUrl", p.getBaseUrl());
        return info;
    }

    private int countEmbeddings(Path chatDir) {
        Path dbPath = chatDir.resolve("metadata.db");
        if (!Files.exists(dbPath))
            return 0;
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM embeddings")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    private int countGraphEdges(Path chatDir) {
        Path dbPath = chatDir.resolve("metadata.db");
        if (!Files.exists(dbPath))
            return 0;
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM memory_graph")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }
}
