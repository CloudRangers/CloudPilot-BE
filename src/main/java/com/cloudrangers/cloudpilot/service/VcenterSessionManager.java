package com.cloudrangers.cloudpilot.monitor.vcenter.service;

import com.cloudrangers.cloudpilot.monitor.vcenter.config.VcenterProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class VcenterSessionManager {

    private final VcenterProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile String cachedSessionId;     // 현재 세션 저장
    private volatile long sessionTimestamp = 0;  // 세션 발급 시각

    public VcenterSessionManager(VcenterProperties properties) {
        this.properties = properties;
    }

    /**
     * 세션이 없거나 오래되었으면 vCenter에서 새로 발급
     */
    public synchronized String getSessionId() {
        long now = System.currentTimeMillis();
        long maxAge = 1000 * 60 * 20; // 20분 유효로 가정 (vCenter는 보통 30분)

        if (cachedSessionId == null || now - sessionTimestamp > maxAge) {
            log.info("🔐 vCenter 세션 갱신 중…");
            cachedSessionId = createNewSession();
            sessionTimestamp = now;
        }

        return cachedSessionId;
    }

    /**
     * Basic Auth로 vCenter 세션 생성
     */
    private String createNewSession() {
        String url = properties.getBaseUrl() + "/session";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(properties.getUsername(), properties.getPassword());
        headers.setAccept(MediaType.parseMediaTypes("application/json"));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("❌ vCenter 세션 생성 실패: {}", response.getStatusCode());
                throw new RuntimeException("vCenter 세션 생성 실패");
            }

            // 응답 body = "세션ID"
            String sessionId = response.getBody().replace("\"", "");
            log.info("✅ vCenter 세션 발급 성공: {}", sessionId);
            return sessionId;

        } catch (Exception e) {
            log.error("🔥 vCenter 세션 생성 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("vCenter 세션 생성 실패", e);
        }
    }
}
