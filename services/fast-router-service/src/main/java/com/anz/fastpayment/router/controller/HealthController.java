package com.anz.fastpayment.router.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for Fast Router Service
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @Value("${app.name}")
    private String appName;

    @Value("${app.version}")
    private String appVersion;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", appName);
        health.put("version", appVersion);
        health.put("timestamp", LocalDateTime.now());
        
        Map<String, String> components = new HashMap<>();
        components.put("kafka", "UP");
        components.put("redis", "UP");
        components.put("spanner", "UP");
        health.put("components", components);
        
        return ResponseEntity.ok(health);
    }
}