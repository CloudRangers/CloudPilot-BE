package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.service.provision.ProvisionSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/sse/provision")
public class ProvisionSseController {

    private final ProvisionSseService sseService;

    /**
     * VM 생성 진행 상태 SSE 구독
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<SseEmitter> subscribe(@PathVariable("jobId") String jobId) {
        log.info("🔌 SSE subscription request for VM provision. jobId={}", jobId);

        SseEmitter emitter = sseService.createEmitter(jobId);

        return ResponseEntity.ok()
                .header("Content-Type", "text/event-stream; charset=UTF-8")
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .body(emitter);
    }
}