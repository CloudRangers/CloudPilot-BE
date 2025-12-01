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

import com.cloudrangers.cloudpilot.ops.domain.metric.MetricTarget;
import com.cloudrangers.cloudpilot.ops.domain.metric.MetricTargetRepository;

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

            String name = defaultString(info.getName(), "vm-" + job.getId());
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

            log.info("[VmProvisionService] vm_instance 저장 완료. jobId={}, vmInstanceId={}, name={}, ip={}",
                    job.getId(), saved.getId(), saved.getName(), saved.getIp());

            // =====================================================================
            // ✅ node-exporter용 MetricTarget 자동 등록
            // =====================================================================
            if (saved.getIp() != null && !saved.getIp().isBlank()) {
                String endpoint = saved.getIp().trim() + ":9100";

                MetricTarget target = new MetricTarget();
                // ⚠️ 아래 필드명은 MetricTarget 엔티티에 맞게 사용
                target.setVmInstanceId(saved.getId());
                target.setEndpoint(endpoint);
                target.setExporterType("NODE_EXPORTER");
                target.setActive(true);
                target.setCreatedAt(Instant.now());
                target.setUpdatedAt(Instant.now());
                // scrapeUrl, labels 컬럼이 있으면 세팅 / 없으면 이 두 줄 지워도 됨
//                try {
//                    target.setScrapeUrl(null);
//                } catch (NoSuchMethodError | RuntimeException ignored) {}
//                try {
//                    target.setLabels(null);
//                } catch (NoSuchMethodError | RuntimeException ignored) {}
//
//                target.setActive(true);
//                target.setCreatedAt(Instant.now());
//                target.setUpdatedAt(Instant.now());

                metricTargetRepository.save(target);

                log.info("[VmProvisionService] MetricTarget 자동등록 완료. vmInstanceId={}, endpoint={}",
                        saved.getId(), endpoint);
            } else {
                log.warn("[VmProvisionService] MetricTarget 자동등록 스킵 - ip 없음. vmInstanceId={}, name={}",
                        saved.getId(), saved.getName());
            }
            // =====================================================================
        }
    }

    private Long resolveZoneId(InstanceInfo info, VmProvisionJob job) {
        if (info.getZoneId() != null) return info.getZoneId();
        if (job.getZoneId() != null) return job.getZoneId().longValue();
        return null;
    }

    /**
     * vm_instance.tags 필드에 저장할 통일된 태그 구조
     */
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
}
