package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.service.pkg.PackageInstallSseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/n8n")
public class N8nController {

    private final PackageInstallSseService sseService;

    @PostMapping("/{jobId}")
    public ResponseEntity<Map<String, String>> pushFromN8n(
            @PathVariable String jobId,
            @RequestBody String rawJson
    ) {
        log.info("📩 [N8N] Received payload for jobId={}: {}", jobId, rawJson);

        try {
            Map<String, Object> parsed =
                    new ObjectMapper().readValue(rawJson, Map.class);

            sseService.sendMessage(jobId, parsed);

            return ResponseEntity.ok(Map.of("status", "sent"));

        } catch (Exception e) {
            log.error("❌ [N8N] JSON parse error", e);
            return ResponseEntity.badRequest().body(Map.of("error", "invalid json"));
        }
    }
}
