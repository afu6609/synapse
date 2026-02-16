package com.synapse.embedding.controller;

import com.synapse.embedding.service.ConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/config")
@CrossOrigin(origins = "*")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * 读取当前配置（Key 脱敏）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(configService.getConfigSnapshot());
    }

    /**
     * 部分更新配置
     */
    @PatchMapping
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> updates) {
        List<String> changedFields = configService.applyConfigUpdate(updates);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("changedFields", changedFields);
        response.put("config", configService.getConfigSnapshot());
        return ResponseEntity.ok(response);
    }

    /**
     * 手动触发维度检测
     */
    @PostMapping("/detect-dimension")
    public ResponseEntity<Map<String, Object>> detectDimension() {
        try {
            Integer dimension = configService.detectDimension();
            return ResponseEntity.ok(Map.of(
                    "detectedDimension", dimension,
                    "config", configService.getConfigSnapshot()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
