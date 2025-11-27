package com.cloudrangers.cloudpilot.service.vm;

import com.cloudrangers.cloudpilot.domain.provision.VmProvisionJob;
import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import com.cloudrangers.cloudpilot.dto.message.ProvisionResultMessage;
import com.cloudrangers.cloudpilot.dto.message.ProvisionResultMessage.InstanceInfo;
import com.cloudrangers.cloudpilot.repository.vm.VmInstanceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VmProvisionService {

    private final VmInstanceRepository vmInstanceRepository;
    private final ObjectMapper objectMapper;

    /**
     * 프로비저닝 성공 시, 결과 메시지에 포함된 instance 정보를
     * vm_instance 테이블에 저장
     */
    @Transactional
    public void handleProvisionSuccess(VmProvisionJob job, ProvisionResultMessage result) {
        List<InstanceInfo> instances = result.getInstances();
        if (instances == null || instances.isEmpty()) {
            log.warn("[VmProvisionService] Job {} - instances 비어 있음. vm_instance 저장 스킵", job.getId());
            return;
        }

        for (InstanceInfo info : instances) {
            Instant now = Instant.now();

            // VM 이름 한 번 계산 (null/blank 시 fallback)
            String name = defaultString(info.getName(), "vm-" + job.getId());

            // 대표 IP (vm_instance.ip와 동일하게 사용)
            String ip = resolveIp(info);

            VmInstance vm = VmInstance.builder()
                    .name(name)
                    .providerType(defaultString(info.getProviderType(), "VSPHERE"))
                    .zoneId(resolveZoneId(info, job))
                    .lifecycle("running")
                    .powerState("ON")
                    .vcpu(info.getCpuCores())
                    .memoryMb(info.getMemoryGb() != null ? info.getMemoryGb() * 1024 : null)
                    .rootDiskGb(info.getDiskGb())
                    .ownerUserId(job.getUserId())
                    .teamId(job.getTeamId())
                    .createdAt(now)
                    .createdBy(job.getUserId())
                    .updatedAt(now)
                    .updatedBy(job.getUserId())
                    .tags(buildTags(info, name, ip))
                    .ip(ip)
                    .build();

            VmInstance saved = vmInstanceRepository.save(vm);

            log.info("[VmProvisionService] vm_instance 저장 완료. jobId={}, vmInstanceId={}, name={}, zoneId={}, ip={}",
                    job.getId(), saved.getId(), saved.getName(), saved.getZoneId(), saved.getIp());
        }
    }

    /**
     * InstanceInfo 또는 Job 에서 zoneId 결정
     */
    private Long resolveZoneId(InstanceInfo info, VmProvisionJob job) {
        if (info.getZoneId() != null) {
            return info.getZoneId();
        }
        if (job.getZoneId() != null) {
            return job.getZoneId().longValue();
        }
        return null;
    }

    /**
     * vm_instance.tags 필드에 저장할 통일된 태그 구조
     *
     * {
     *   "externalId": "vm-473",
     *   "vmName": "Vmprovision-db-ip-test123123",
     *   "primaryIp": "172.16.5.109",
     *   "nicIps": ["172.16.5.109"],
     *   "osType": "ubuntu"
     * }
     */
    private String buildTags(InstanceInfo info, String vmName, String primaryIp) {
        Map<String, Object> m = new LinkedHashMap<>();

        // 1) vSphere VM ID (MoRef 또는 UUID)
        if (info.getExternalId() != null && !info.getExternalId().isBlank()) {
            m.put("externalId", info.getExternalId().trim());
        }

        // 2) VM 이름 (CloudPilot 상 이름과 동일)
        m.put("vmName", vmName);

        // 3) 대표 IP (vm_instance.ip와 동일)
        if (primaryIp != null && !primaryIp.isBlank()) {
            m.put("primaryIp", primaryIp.trim());
        }

        // 4) NIC 별 IP 리스트
        List<String> nicIps = parseNicAddresses(info.getNicAddresses());
        if (!nicIps.isEmpty()) {
            m.put("nicIps", nicIps);
        }

        // 5) OS 타입
        if (info.getOsType() != null && !info.getOsType().isBlank()) {
            m.put("osType", info.getOsType().trim());
        }

        if (m.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(m);
        } catch (JsonProcessingException e) {
            log.warn("[VmProvisionService] tags JSON 직렬화 실패. name={}, err={}",
                    info.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * "172.16.0.10,172.16.0.11" → ["172.16.0.10", "172.16.0.11"]
     */
    private List<String> parseNicAddresses(String nicAddresses) {
        List<String> result = new ArrayList<>();
        if (nicAddresses == null || nicAddresses.isBlank()) {
            return result;
        }

        String[] parts = nicAddresses.split(",");
        for (String part : parts) {
            String ip = part.trim();
            if (!ip.isEmpty()) {
                result.add(ip);
            }
        }
        return result;
    }

    /**
     * 팀 네트워크용 대표 IP 결정 로직
     * 1) ipAddress 가 있으면 그걸 사용
     * 2) 없으면 nicAddresses 에서 첫 번째 IP 사용
     */
    private String resolveIp(InstanceInfo info) {
        if (info.getIpAddress() != null && !info.getIpAddress().isBlank()) {
            return info.getIpAddress().trim();
        }

        List<String> nicList = parseNicAddresses(info.getNicAddresses());
        if (!nicList.isEmpty()) {
            return nicList.get(0);
        }

        return null;
    }

    private String defaultString(String value, String def) {
        return (value == null || value.isBlank()) ? def : value;
    }
}
