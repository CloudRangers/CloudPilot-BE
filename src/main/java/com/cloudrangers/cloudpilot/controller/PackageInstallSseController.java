package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.service.pkg.PackageInstallSseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/sse")
public class PackageInstallSseController {

    private final PackageInstallSseService sseService;

    @GetMapping("/{jobId}")
    public ResponseEntity<SseEmitter> subscribe(@PathVariable String jobId) {

        log.info("🔌 SSE subscription request. jobId={}", jobId);

        SseEmitter emitter = sseService.createEmitter(jobId);

        return ResponseEntity.ok()
                .header("Content-Type", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .body(emitter);
    }

    @PostMapping("/n8n/{jobId}")
    public ResponseEntity<Map<String, String>> pushFromN8n(
            @PathVariable String jobId,
            @RequestBody String rawJson
    ) {
        log.info("📩 Received N8N payload for jobId={}: {}", jobId, rawJson);

        try {
            Map<String, Object> parsed = new ObjectMapper()
                    .readValue(rawJson, Map.class);

            sseService.sendMessage(jobId, parsed);

            return ResponseEntity.ok(Map.of("status", "sent"));
        } catch (Exception e) {
            log.error("❌ JSON parse error", e);
            return ResponseEntity.badRequest().body(Map.of("error", "invalid json"));
        }
    }

}
