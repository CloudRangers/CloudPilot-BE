package com.cloudrangers.cloudpilot.infra.vcenter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VCenterClient {

    // -----------------------------
    // vCenter 설정 (application.properties)
    // -----------------------------
    @Value("${vcenter.base-url}")
    private String baseUrl;

    @Value("${vcenter.username}")
    private String username;

    @Value("${vcenter.password}")
    private String password;

    // SSL 무시 RestTemplate (RestTemplateConfig에서 등록됨)
    private final RestTemplate restTemplate;

    // -----------------------------
    // 세션 발급
    // -----------------------------
    public String obtainSessionId() {
        String url = baseUrl + "/session";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);
        headers.set("vmware-use-header-authn", "true");
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            log.info("[vCenter] POST {}", url);

            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("[vCenter] 세션 요청 실패: status={}, body={}",
                        response.getStatusCode(), response.getBody());
                throw new IllegalStateException("vCenter 세션 생성 실패");
            }

            // 헤더에서 세션 ID 추출
            List<String> headerValues = response.getHeaders().get("vmware-api-session-id");
            if (headerValues != null && !headerValues.isEmpty()) {
                return headerValues.get(0);
            }

            // body fallback (간혹 vSphere 버전에 따라 body에 session id가 나오기도 함)
            if (response.getBody() != null && !response.getBody().isBlank()) {
                return response.getBody().replace("\"", "").trim();
            }

            throw new IllegalStateException("vCenter 세션 ID를 찾을 수 없습니다.");

        } catch (RestClientException e) {
            log.error("[vCenter] 세션 요청 중 예외 발생", e);
            throw new IllegalStateException("vCenter 세션 요청 중 오류 발생", e);
        }
    }

    // -----------------------------
    // VM 목록 조회
    // -----------------------------
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listVms() {

        String sessionId = obtainSessionId();
        String url = baseUrl + "/vcenter/vm";

        HttpHeaders headers = new HttpHeaders();
        headers.set("vmware-api-session-id", sessionId);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            log.info("[vCenter] GET {}", url);

            ResponseEntity<List> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, List.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("[vCenter] VM 조회 실패: status={}, body={}",
                        response.getStatusCode(), response.getBody());
                throw new IllegalStateException("vCenter VM 조회 실패");
            }

            List<Map<String, Object>> body = response.getBody();
            return body != null ? body : Collections.emptyList();

        } catch (RestClientException e) {
            log.error("[vCenter] VM 조회 중 예외 발생", e);
            throw new IllegalStateException("vCenter VM 조회 중 오류 발생", e);
        }
    }

    // -----------------------------
    // Host 목록 조회
    // -----------------------------
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listHosts() {

        String sessionId = obtainSessionId();
        String url = baseUrl + "/vcenter/host";

        HttpHeaders headers = new HttpHeaders();
        headers.set("vmware-api-session-id", sessionId);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            log.info("[vCenter] GET {}", url);

            ResponseEntity<List> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, List.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("[vCenter] Host 조회 실패: status={}, body={}",
                        response.getStatusCode(), response.getBody());
                throw new IllegalStateException("vCenter Host 조회 실패");
            }

            List<Map<String, Object>> body = response.getBody();
            return body != null ? body : Collections.emptyList();

        } catch (RestClientException e) {
            log.error("[vCenter] Host 조회 중 예외 발생", e);
            return Collections.emptyList();  // Host는 없어도 summary 계산 가능
        }
    }
}
