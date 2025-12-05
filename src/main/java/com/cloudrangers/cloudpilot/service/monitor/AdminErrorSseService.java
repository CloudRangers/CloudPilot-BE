package com.cloudrangers.cloudpilot.service.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AdminErrorSseService {

    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String adminKey) {
        SseEmitter emitter = new SseEmitter(0L); // 무제한 (원하면 타임아웃 설정 가능)

        emitterMap.put(adminKey, emitter);
        log.info("🔌 Admin SSE emitter created. adminKey={}", adminKey);

        emitter.onCompletion(() -> {
            emitterMap.remove(adminKey);
            log.info("✅ Admin SSE emitter completed. adminKey={}", adminKey);
        });

        emitter.onTimeout(() -> {
            emitterMap.remove(adminKey);
            log.warn("⏱ Admin SSE emitter timeout. adminKey={}", adminKey);
        });

        emitter.onError(e -> {
            emitterMap.remove(adminKey);
            log.error("❌ Admin SSE emitter error. adminKey={}", adminKey, e);
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("init")
                    .data("connected"));
        } catch (IOException e) {
            log.error("❌ Failed to send init event. adminKey={}", adminKey, e);
        }

        return emitter;
    }

    public void broadcast(Map<String, Object> payload) {
        log.info("📡 Broadcasting admin error to {} emitters", emitterMap.size());

        emitterMap.forEach((adminKey, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(payload));
            } catch (IOException e) {
                log.error("❌ Failed to send SSE to adminKey={}", adminKey, e);
                emitterMap.remove(adminKey);
            }
        });
    }
}
