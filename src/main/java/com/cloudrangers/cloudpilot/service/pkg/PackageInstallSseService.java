package com.cloudrangers.cloudpilot.service.pkg;

import com.cloudrangers.cloudpilot.dto.message.InstallPackageProgressMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PackageInstallSseService {

    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    // 프론트가 SSE 연결할 때 생성
    public SseEmitter createEmitter(String jobId) {
        SseEmitter emitter = new SseEmitter(0L); // timeout 없음

        emitter.onCompletion(() -> emitterMap.remove(jobId));
        emitter.onTimeout(() -> emitterMap.remove(jobId));
        emitter.onError((e) -> emitterMap.remove(jobId));

        emitterMap.put(jobId, emitter);
        log.info("🔌 SSE emitter created. jobId={}", jobId);

        return emitter;
    }

    // Worker → RabbitMQ → Listener에서 호출
    public void sendProgress(InstallPackageProgressMessage msg) {
        String jobId = msg.getJobId();
        SseEmitter emitter = emitterMap.get(jobId);

        if (emitter == null) {
            log.warn("⚠ SSE emitter not found for jobId={}", jobId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(msg));
        } catch (IOException e) {
            log.error("❌ SSE send failed, removing emitter. jobId={}", jobId, e);
            emitterMap.remove(jobId);
        }
    }
}
