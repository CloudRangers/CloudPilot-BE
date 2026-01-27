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

import com.cloudrangers.cloudpilot.repository.MetricTargetRepository;

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
    private final MetricTargetRepository metricTargetRepository;   // ✅ 추가

    /**
     * 프로비저닝 성공 시, 결과 메시지에 포함된 instance 정보를
     * vm_instance 테이블에 저장
     *
     * - tf_run_id  : 워커가 result.tfRunId에 넣어준 값 (없으면 null)
     * - state_uri  : BE에서 jobId 기준으로 생성 (/tmp/terraform/{jobId}/terraform.tfstate)
     */
    @Transactional
    public void handleProvisionSuccess(VmProvisionJob job, ProvisionResultMessage result) {
        List<InstanceInfo> instances = result.getInstances();

        log.info("[VmProvisionService] handleProvisionSuccess 호출. jobId={}, resultStatus={}, step={}, instanceCount={}",
                job.getId(),
                result.getStatus(),
                result.getStep(),
                (instances == null ? 0 : instances.size()));

        if (instances == null || instances.isEmpty()) {
            log.warn("[VmProvisionService] Job {} - instances 비어 있음. vm_instance 저장 스킵", job.getId());
            return;
        }

        Long tfRunId = result.getTfRunId();
        String stateUri = buildTerraformStateUri(job.getId());

        log.info("[VmProvisionService] jobId={}, tfRunId={}, stateUri={}",
                job.getId(), tfRunId, stateUri);

        try {
            for (InstanceInfo info : instances) {
                Instant now = Instant.now();

                String name = defaultString(info.getName(), "vm-" + job.getId());
                String ip = resolveIp(info);
                String providerInstanceId = normalizeBlankToNull(info.getExternalId());

                Long zoneId = resolveZoneId(info, job);

                log.debug("[VmProvisionService] vm_instance 생성 준비. jobId={}, name={}, ip={}, zoneId={}, providerInstanceId={}",
                        job.getId(), name, ip, zoneId, providerInstanceId);

                VmInstance vm = VmInstance.builder()
                        .name(name)
                        .providerType(defaultString(info.getProviderType(), "VSPHERE"))

                        // 위치/소유 정보
                        .zoneId(zoneId)
                        .ownerUserId(job.getUserId())
                        .teamId(job.getTeamId())

                        // 라이프사이클/전원 상태
                        .lifecycle("running")
                        .powerState("ON")

                        // 스펙
                        .vcpu(info.getCpuCores())
                        .memoryMb(info.getMemoryGb() != null ? info.getMemoryGb() * 1024 : null)
                        .rootDiskGb(info.getDiskGb())

                        // 연동 정보
                        .tfRunId(tfRunId)
                        .stateUri(stateUri)

                        // 네트워크
                        .ip(ip)

                        // 태그(JSON)
                        .tags(buildTags(info, name, ip))

                        // 메타데이터
                        .createdAt(now)
                        .createdBy(job.getUserId())
                        .updatedAt(now)
                        .updatedBy(job.getUserId())
                        .build();

                VmInstance saved = vmInstanceRepository.save(vm);

                log.info(
                        "[VmProvisionService] vm_instance 저장 완료. jobId={}, vmInstanceId={}, name={}, zoneId={}, ip={}, tfRunId={}, stateUri={}",
                        job.getId(), saved.getId(), saved.getName(), saved.getZoneId(), saved.getIp(),
                        saved.getTfRunId(), saved.getStateUri()
                );
            }
        } catch (Exception e) {
            log.error("[VmProvisionService] vm_instance 저장 중 예외 발생. jobId={}", job.getId(), e);
            throw e; // 트랜잭션 롤백되게 그대로 다시 던짐
        }
    }

    private Long resolveZoneId(InstanceInfo info, VmProvisionJob job) {
        if (info.getZoneId() != null) {
            return info.getZoneId();
        }
        if (job.getZoneId() != null) {
            return job.getZoneId().longValue();
        }
        return null;
    }

    private String buildTags(InstanceInfo info, String vmName, String primaryIp) {
        Map<String, Object> m = new LinkedHashMap<>();

        if (info.getExternalId() != null && !info.getExternalId().isBlank()) {
            m.put("externalId", info.getExternalId().trim());
        }

        m.put("vmName", vmName);

        if (primaryIp != null && !primaryIp.isBlank()) {
            m.put("primaryIp", primaryIp.trim());
        }

        List<String> nicIps = parseNicAddresses(info.getNicAddresses());
        if (!nicIps.isEmpty()) {
            m.put("nicIps", nicIps);
        }

        if (info.getOsType() != null && !info.getOsType().isBlank()) {
            m.put("osType", info.getOsType().trim());
        }

        if (info.getOsType() != null && !info.getOsType().isBlank()) {
            m.put("osType", info.getOsType().trim());
        }

        if (m.isEmpty()) return null;

        try {
            return objectMapper.writeValueAsString(m);
        } catch (JsonProcessingException e) {
            log.warn("[VmProvisionService] tags JSON 직렬화 실패. name={}, err={}",
                    info.getName(), e.getMessage());
            return null;
        }
    }

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

    private String normalizeBlankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String buildTerraformStateUri(Long jobId) {
        if (jobId == null) {
            return null;
        }
        return "/tmp/terraform/" + jobId + "/terraform.tfstate";
    }
}
