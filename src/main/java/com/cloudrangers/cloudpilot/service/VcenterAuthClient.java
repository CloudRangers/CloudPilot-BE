package com.cloudrangers.cloudpilot.service;

import com.cloudrangers.cloudpilot.config.VcenterProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class VcenterAuthClient {

    private final VcenterProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * vCenter 세션 발급 (session-id 반환)
     */
    public String createSession() {
        String url = properties.getBaseUrl() + "/session";

        String auth = properties.getUsername() + ":" + properties.getPassword();
        String base64 = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + base64);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String newSessionId = response.getBody().replace("\"", "").trim();
                properties.setSessionId(newSessionId);
                log.info("vCenter 세션 재발급 성공: {}", newSessionId);
                return newSessionId;
            }
        } catch (Exception e) {
            log.error("vCenter 세션 발급 실패: {}", e.getMessage());
        }

        return null;
    }

}
