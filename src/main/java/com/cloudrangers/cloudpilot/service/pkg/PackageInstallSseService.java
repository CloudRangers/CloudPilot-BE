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


    public SseEmitter createEmitter(String jobId) {
        SseEmitter emitter = new SseEmitter(0L);

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

        List<InstallPackageProgressMessage> cachedMessages = earlyMessageCache.get(jobId);
        if (cachedMessages != null) {
            log.info("▶ Found {} cached messages for jobId={}. Sending now.", cachedMessages.size(), jobId);
            synchronized (cachedMessages) {
                for (InstallPackageProgressMessage msg : cachedMessages) {
                    sendProgressInternal(emitter, msg);
                }
            }
            earlyMessageCache.remove(jobId);
        }

        return emitter;
    }

    private void sendProgressInternal(SseEmitter emitter, InstallPackageProgressMessage msg) {
        String jobId = msg.getJobId();

        try {
            // 1. Progress 이벤트 전송
            emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(msg));

            // 2. Job 완료 시 Complete 이벤트 전송 및 Emitter 종료
            // 'int' 타입이므로 null 체크를 제거하고 바로 100과 비교합니다.
            if (msg.getProgress() == 100) {
                log.info("🎉 Sending complete event for jobId={}", jobId);

                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(msg));

                emitter.complete();
            }

        } catch (IOException e) {
            log.error("❌ SSE send failed, removing emitter. jobId={}", jobId, e);
            emitterMap.remove(jobId);
            earlyMessageCache.remove(jobId);
        }
    }


    public void sendProgress(InstallPackageProgressMessage msg) {
        String jobId = msg.getJobId();
        SseEmitter emitter = emitterMap.get(jobId);

        if (emitter == null) {
            log.warn("⚠ SSE emitter not found for jobId={}. Caching message.", jobId);

            earlyMessageCache.computeIfAbsent(jobId, k -> new CopyOnWriteArrayList<>()).add(msg);

            return;
        }

        sendProgressInternal(emitter, msg);
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