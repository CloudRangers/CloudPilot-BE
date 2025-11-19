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
     * vm_instance 테이블에 저장.
     */
    @Transactional
    public void handleProvisionSuccess(VmProvisionJob job, ProvisionResultMessage result) {
        List<InstanceInfo> instances = result.getInstances();
        if (instances == null || instances.isEmpty()) {
            log.warn("[VmProvisionService] Job {} - instances 비어 있음. vm_instance 저장 스킵", job.getId());
            return;
        }

        for (InstanceInfo info : instances) {
            VmInstance vm = VmInstance.builder()
                    // vm 이름
                    .name(defaultString(info.getName(), "vm-" + job.getId()))

                    // provider / zone
                    .providerType(defaultString(info.getProviderType(), "VSPHERE"))
                    .zoneId(resolveZoneId(info, job))

                    // 상태
                    .lifecycle("running")  // creating → running
                    .powerState("ON")

                    // 스펙
                    .vcpu(info.getCpuCores())
                    .memoryMb(info.getMemoryGb() != null ? info.getMemoryGb() * 1024 : null) // GB → MB
                    .rootDiskGb(info.getDiskGb())

                    // 소유자 정보
                    .ownerUserId(job.getUserId())
                    .teamId(job.getTeamId())

                    // 생성 시간
                    .createdAt(Instant.now())

                    // 부가 정보(JSON)
                    .tags(buildTags(info))
                    .build();

            VmInstance saved = vmInstanceRepository.save(vm);
            log.info("[VmProvisionService] vm_instance 저장 완료. jobId={}, vmInstanceId={}, name={}, zoneId={}",
                    job.getId(), saved.getId(), saved.getName(), saved.getZoneId());
        }
    }

    private Long resolveZoneId(InstanceInfo info, VmProvisionJob job) {
        if (info.getZoneId() != null) return info.getZoneId();
        if (job.getZoneId() != null) return job.getZoneId().longValue();
        return null;
    }

    /**
     * vm_instance.tags 필드에 externalId / ip / osType 정도를 JSON으로 묶어서 저장.
     * (현재 VmInstance 엔티티에 별도 필드가 없으니 tags에 몰빵)
     */
    private String buildTags(InstanceInfo info) {
        Map<String, Object> m = new LinkedHashMap<>();

        if (info.getExternalId() != null) m.put("externalId", info.getExternalId());
        if (info.getIpAddress() != null)  m.put("ipAddress", info.getIpAddress());
        if (info.getOsType() != null)     m.put("osType", info.getOsType());

        if (m.isEmpty()) return null;

        try {
            return objectMapper.writeValueAsString(m);
        } catch (JsonProcessingException e) {
            log.warn("[VmProvisionService] tags JSON 직렬화 실패. name={}, err={}",
                    info.getName(), e.getMessage());
            return null;
        }
    }

    private String defaultString(String value, String def) {
        return (value == null || value.isBlank()) ? def : value;
    }
}
