package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.security.AuthUtil;
import com.cloudrangers.cloudpilot.service.monitor.AdminErrorSseService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/sse/admin")
public class AdminErrorSseController {

    private final AdminErrorSseService adminErrorSseService;
    private final ObjectMapper objectMapper;

    /**
     * 🔥 관리자 전용 vSphere/시스템 에러 SSE 구독
     */
    @GetMapping("/vsphere-error")
    public ResponseEntity<SseEmitter> subscribeVsphereError() {

        if (!AuthUtil.isAuthenticated()) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }

        String role = AuthUtil.getRole();
        if (!"ADMIN".equals(role)) {
            throw new AccessDeniedException("관리자만 구독할 수 있습니다.");
        }

        Long userId = AuthUtil.getUserId();
        String adminKey = "admin-" + (userId != null ? userId : "unknown");

        log.info("🔌 Admin SSE subscribe. adminKey={}", adminKey);

        SseEmitter emitter = adminErrorSseService.createEmitter(adminKey);

        return ResponseEntity.ok()
                .header("Content-Type", "text/event-stream; charset=UTF-8")
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .body(emitter);
    }

    /**
     * 🔁 n8n → AI 분석 결과 → 관리자 브라우저로 브로드캐스트
     *   POST /sse/admin/vsphere-error/publish
     */
    @PostMapping("/vsphere-error/publish")
    public ResponseEntity<Map<String, String>> publishFromN8n(
            @RequestBody(required = false) String rawBody
    ) throws IOException {

        log.info("▶ vSphere AI error from n8n (raw): {}", rawBody);

        Map<String, Object> payload;

        if (rawBody == null || rawBody.isBlank()) {
            payload = Collections.singletonMap("message", "EMPTY_BODY_FROM_N8N");
        } else {
            String cleaned = rawBody.trim();

            // ✅ 반복적으로 정제 (따옴표 안에 =가 있을 수 있으므로)
            for (int i = 0; i < 3; i++) {
                // 따옴표로 감싸진 JSON 문자열인 경우 먼저 언래핑
                if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
                    try {
                        cleaned = objectMapper.readValue(cleaned, String.class);
                        log.info("▶ [{}] Unwrapped quoted JSON string", i);
                    } catch (Exception e) {
                        log.warn("▶ [{}] Failed to unwrap quoted string", i);
                        break;
                    }
                }

                // '='가 앞에 붙어있으면 제거
                if (cleaned.startsWith("=")) {
                    cleaned = cleaned.substring(1);
                    log.info("▶ [{}] Removed leading '='", i);
                }

                // 이미 유효한 JSON이면 루프 종료
                if (cleaned.startsWith("{") || cleaned.startsWith("[")) {
                    break;
                }
            }

            log.info("▶ Final cleaned body: {}", cleaned.substring(0, Math.min(200, cleaned.length())));

            // JSON 파싱
            JsonNode node = objectMapper.readTree(cleaned);

            // 배열이면 첫 번째 요소 추출
            if (node.isArray() && node.size() > 0) {
                node = node.get(0);
            }

            if (node.isObject()) {
                payload = objectMapper.convertValue(
                        node,
                        new TypeReference<Map<String, Object>>() {}
                );
            } else {
                payload = Collections.singletonMap("message", node.toString());
            }
        }

        log.info("▶ vSphere AI error normalized payload keys: {}", payload.keySet());
        adminErrorSseService.broadcast(payload);

        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}