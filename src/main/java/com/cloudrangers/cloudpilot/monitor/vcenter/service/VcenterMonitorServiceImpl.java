package com.cloudrangers.cloudpilot.monitor.vcenter.service;

import com.cloudrangers.cloudpilot.monitor.vcenter.config.VcenterProperties;
import com.cloudrangers.cloudpilot.monitor.vcenter.dto.VcenterSummaryResponse;
import com.cloudrangers.cloudpilot.monitor.vcenter.dto.VcenterVmInfoDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class VcenterMonitorServiceImpl implements VcenterMonitorService {

    private final VcenterProperties properties;
    private final VcenterSessionManager sessionManager;  // 세션 자동 관리용

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VcenterMonitorServiceImpl(VcenterProperties properties,
                                     VcenterSessionManager sessionManager) {
        this.properties = properties;
        this.sessionManager = sessionManager;
    }

    /**
     * vCenter REST API /vcenter/vm 호출해서 JSON 결과 파싱
     * - 현재 실제 응답: [ { vm, name, power_state, cpu_count, memory_size_MiB, ... }, ... ]
     * - 혹시 { "value": [ ... ] } 형태도 같이 지원
     */
    private List<VcenterVmInfoDto> fetchVmInfosFromVcenter() {
        String url = properties.getBaseUrl() + "/vcenter/vm";

        HttpHeaders headers = new HttpHeaders();
        // 세션 매니저에서 최신 세션 ID 가져오기
        headers.set("vmware-api-session-id", sessionManager.getSessionId());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("vCenter VM API 호출 실패: status={}, body={}",
                        response.getStatusCode(), response.getBody());
                return List.of();
            }

            String body = response.getBody();
            JsonNode root = objectMapper.readTree(body);

            JsonNode vmArrayNode;

            // 1) 현재처럼 루트가 바로 배열인 경우
            if (root.isArray()) {
                vmArrayNode = root;
            }
            // 2) 혹시 { "value": [ ... ] } 형태도 지원
            else if (root.has("value") && root.get("value").isArray()) {
                vmArrayNode = root.get("value");
            } else {
                log.warn("vCenter VM 응답 형식이 예상과 다름: {}", body);
                return List.of();
            }

            List<VcenterVmInfoDto> result = new ArrayList<>();

            for (JsonNode vmNode : vmArrayNode) {
                String vmId = vmNode.path("vm").asText(null);
                String name = vmNode.path("name").asText(null);
                String powerState = vmNode.path("power_state").asText(null);

                Integer cpuCount = vmNode.has("cpu_count")
                        ? vmNode.get("cpu_count").asInt()
                        : null;

                Long memorySizeMiB = vmNode.has("memory_size_MiB")
                        ? vmNode.get("memory_size_MiB").asLong()
                        : null;

                // guest_OS / ip_address 는 지금 응답에 없으니 null로 들어갈 수 있음
                String guestOs = vmNode.path("guest_OS").asText(null);
                String ipAddress = vmNode.path("ip_address").asText(null);

                VcenterVmInfoDto dto = VcenterVmInfoDto.builder()
                        .vmId(vmId)
                        .name(name)
                        .powerState(powerState)
                        .cpuCount(cpuCount)
                        .memorySizeMiB(memorySizeMiB)
                        .guestOs(guestOs)
                        .ipAddress(ipAddress)
                        .build();

                result.add(dto);
            }

            return result;

        } catch (Exception e) {
            log.error("vCenter VM 목록 조회 중 예외 발생: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public List<VcenterVmInfoDto> getVmList() {
        return fetchVmInfosFromVcenter();
    }

    @Override
    public VcenterSummaryResponse getSummary() {
        List<VcenterVmInfoDto> vms = fetchVmInfosFromVcenter();

        int total = vms.size();
        int poweredOn = 0;
        int poweredOff = 0;
        int suspended = 0;
        int unknown = 0;

        for (VcenterVmInfoDto vm : vms) {
            String state = vm.getPowerState();
            if (state == null) {
                unknown++;
                continue;
            }

            switch (state) {
                case "POWERED_ON" -> poweredOn++;
                case "POWERED_OFF" -> poweredOff++;
                case "SUSPENDED" -> suspended++;
                default -> unknown++;
            }
        }

        return VcenterSummaryResponse.builder()
                .totalVms(total)
                .poweredOn(poweredOn)
                .poweredOff(poweredOff)
                .suspended(suspended)
                .unknown(unknown)
                .build();
    }
}
