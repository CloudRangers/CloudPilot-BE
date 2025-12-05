package com.cloudrangers.cloudpilot.service.provision;

import com.cloudrangers.cloudpilot.dto.message.ProvisionProgressMessage;
import com.cloudrangers.cloudpilot.dto.message.ProvisionResultMessage; // ⭐ 추가
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProvisionSseService {

    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    /**
     * 프론트엔드가 SSE 연결할 때 Emitter 생성
     */
    public SseEmitter createEmitter(String jobId) {
        SseEmitter emitter = new SseEmitter(0L); // timeout 없음

        emitter.onCompletion(() -> {
            log.info("🔌 SSE connection completed. jobId={}", jobId);
            emitterMap.remove(jobId);
        });

        emitter.onTimeout(() -> {
            log.warn("⏰ SSE connection timeout. jobId={}", jobId);
            emitterMap.remove(jobId);
        });

        emitter.onError((e) -> {
            log.error("❌ SSE connection error. jobId={}", jobId, e);
            emitterMap.remove(jobId);
        });

        emitterMap.put(jobId, emitter);
        log.info("✅ SSE emitter created. jobId={}", jobId);

        return emitter;
    }

    /**
     * Worker → RabbitMQ → Listener에서 호출
     * 진행 상태를 프론트엔드로 전송
     */
    public void sendProgress(ProvisionProgressMessage msg) {
        String jobId = msg.getJobId();
        SseEmitter emitter = emitterMap.get(jobId);

        if (emitter == null) {
            log.warn("⚠ SSE emitter not found for jobId={}", jobId);
            return;
        }

        try {
            String jsonData = objectMapper.writeValueAsString(msg);

            emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(jsonData, MediaType.APPLICATION_JSON));

            log.debug("📤 SSE progress sent. jobId={}, stage={}, progress={}%",
                    jobId, msg.getStage(), msg.getProgress());

        } catch (IOException e) {
            log.error("❌ SSE send failed, removing emitter. jobId={}", jobId, e);
            emitterMap.remove(jobId);
        }
    }

    /**
     * 완료 이벤트 전송 후 연결 종료
     */
    public void sendComplete(ProvisionProgressMessage msg) {
        String jobId = msg.getJobId();
        SseEmitter emitter = emitterMap.get(jobId);

        if (emitter == null) {
            log.warn("⚠ SSE emitter not found for jobId={}", jobId);
            return;
        }

        try {
            String jsonData = objectMapper.writeValueAsString(msg);

            emitter.send(SseEmitter.event()
                    .name("complete")
                    .data(jsonData, MediaType.APPLICATION_JSON));

            log.info("✅ SSE complete sent. jobId={}", jobId);

            // 완료 후 연결 종료
            emitter.complete();
            emitterMap.remove(jobId);

        } catch (IOException e) {
            log.error("❌ SSE complete failed. jobId={}", jobId, e);
            emitterMap.remove(jobId);
        }
    }

    /**
     * 에러 이벤트 전송
     */
    public void sendError(String jobId, String errorMessage) {
        SseEmitter emitter = emitterMap.get(jobId);

        if (emitter == null) {
            log.warn("⚠ SSE emitter not found for jobId={}", jobId);
            return;
        }

        try {

            ProvisionProgressMessage errorMsg = ProvisionProgressMessage.builder()
                    .jobId(jobId)
                    .stage("ERROR")
                    .description(errorMessage)
                    .status("FAILED")
                    .build();

            String jsonData = objectMapper.writeValueAsString(errorMsg);

            emitter.send(SseEmitter.event()
                    .name("provision-error")
                    .data(jsonData, MediaType.APPLICATION_JSON));

            log.error("❌ SSE error sent. jobId={}, error={}", jobId, errorMessage);

            // 에러 후 연결 종료
            emitter.complete();
            emitterMap.remove(jobId);

        } catch (IOException e) {
            log.error("❌ SSE error send failed. jobId={}", jobId, e);
            emitterMap.remove(jobId);
        }
    }

}
