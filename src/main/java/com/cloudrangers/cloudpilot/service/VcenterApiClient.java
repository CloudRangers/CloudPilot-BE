package com.cloudrangers.cloudpilot.service;

import com.cloudrangers.cloudpilot.config.VcenterProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class VcenterApiClient {

    private final VcenterProperties properties;
    private final VcenterAuthClient authClient;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 공통 호출: 401 나오면 → 세션 갱신 → 자동 재요청
     */
    public ResponseEntity<String> get(String path) {

        String url = properties.getBaseUrl() + path;

        HttpHeaders headers = new HttpHeaders();
        headers.set("vmware-api-session-id", properties.getSessionId());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("vCenter 세션 만료 → 세션 재발급 시도");

            String newId = authClient.createSession();
            if (newId == null) {
                log.error("vCenter 세션 재발급 실패");
                throw e;
            }

            // 재요청
            headers.set("vmware-api-session-id", newId);
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        }
    }
}
