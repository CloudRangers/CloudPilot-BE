package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.service.pkg.PackageInstallSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/sse/package-install")
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
}
