package com.cloudrangers.cloudpilot.service.pkg;

import com.cloudrangers.cloudpilot.dto.message.InstallPackageProgressMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class PackageInstallSseService {

    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    private final Map<String, List<InstallPackageProgressMessage>> earlyMessageCache = new ConcurrentHashMap<>();


    // 프론트가 SSE 연결할 때 생성
    public SseEmitter createEmitter(String jobId) {
        SseEmitter emitter = new SseEmitter(0L); // timeout 없음

        emitter.onCompletion(() -> {
            log.info("🔌 SSE emitter completed. jobId={}", jobId);
            emitterMap.remove(jobId);
            earlyMessageCache.remove(jobId);
        });
        emitter.onTimeout(() -> {
            log.warn("🔌 SSE emitter timed out. jobId={}", jobId);
            emitterMap.remove(jobId);
            earlyMessageCache.remove(jobId);
        });
        emitter.onError((e) -> {
            log.error("🔌 SSE emitter error. jobId={}", jobId, e);
            emitterMap.remove(jobId);
            earlyMessageCache.remove(jobId);
        });


        emitterMap.put(jobId, emitter);
        log.info("🔌 SSE emitter created. jobId={}", jobId);

        // 캐시된 메시지가 있으면 즉시 전송
        List<InstallPackageProgressMessage> cachedMessages = earlyMessageCache.get(jobId);
        if (cachedMessages != null) {
            log.info("▶ Found {} cached messages for jobId={}. Sending now.", cachedMessages.size(), jobId);
            // 순서 보장을 위해 동기적으로 처리
            synchronized (cachedMessages) {
                for (InstallPackageProgressMessage msg : cachedMessages) {
                    sendProgress(msg);
                }
            }
            earlyMessageCache.remove(jobId);
        }

        return emitter;
    }

    // Worker → RabbitMQ → Listener에서 호출
    public void sendProgress(InstallPackageProgressMessage msg) {
        String jobId = msg.getJobId();
        SseEmitter emitter = emitterMap.get(jobId);

        if (emitter == null) {
            // Emitter가 없으면 캐시에 저장
            log.warn("⚠ SSE emitter not found for jobId={}. Caching message.", jobId);
            earlyMessageCache.computeIfAbsent(jobId, k -> new CopyOnWriteArrayList<>()).add(msg);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(msg));
        } catch (IOException e) {
            log.error("❌ SSE send failed, removing emitter. jobId={}", jobId, e);
            emitterMap.remove(jobId);
            earlyMessageCache.remove(jobId);
        }
    }

    public void sendMessage(String jobId, Object data) {
        SseEmitter emitter = emitterMap.get(jobId);

        if (emitter == null) {
            log.warn("⚠ SSE emitter not found for jobId={}", jobId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(data));
        } catch (IOException e) {
            log.error("❌ SSE send failed, removing emitter. jobId={}", jobId, e);
            emitterMap.remove(jobId);
        }
    }

}
