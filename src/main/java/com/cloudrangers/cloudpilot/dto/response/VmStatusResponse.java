package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.catalog.OsImage;
import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
public class VmStatusResponse {

    private final Long id;
    private final String name;
    private final String providerType;
    private final Long zoneId;
    private final String status;
    private final String powerState;
    private final Instant createdAt;

    private final Integer vcpu;
    private final Integer memoryMb;
    private final Integer rootDiskGb;

    private final String ip;
    private final String osImageName;
    private final String osType;

    @Builder
    public VmStatusResponse(Long id,
                            String name,
                            String providerType,
                            Long zoneId,
                            String status,
                            String powerState,
                            Instant createdAt,
                            Integer vcpu,
                            Integer memoryMb,
                            Integer rootDiskGb,
                            String ip,
                            String osImageName,
                            String osType) {
        this.id = id;
        this.name = name;
        this.providerType = providerType;
        this.zoneId = zoneId;
        this.status = status;
        this.powerState = powerState;
        this.createdAt = createdAt;
        this.vcpu = vcpu;
        this.memoryMb = memoryMb;
        this.rootDiskGb = rootDiskGb;
        this.ip = ip;
        this.osImageName = osImageName;
        this.osType = osType;
    }

    public static VmStatusResponse fromEntity(VmInstance vm, OsImage osImage) {
        if (vm == null) return null;

        String ip = vm.getIp();
        String osType = null;

        // tags JSON 파싱
        if (vm.getTags() != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(vm.getTags());

                if (node.has("ipAddress") && (ip == null || ip.isBlank())) {
                    ip = node.get("ipAddress").asText();
                }
                if (node.has("osType")) {
                    osType = node.get("osType").asText();
                }
            } catch (Exception ignored) {
            }
        }

        return VmStatusResponse.builder()
                .id(vm.getId())
                .name(vm.getName())
                .providerType(vm.getProviderType())
                .zoneId(vm.getZoneId())
                .status(vm.getLifecycle())
                .powerState(vm.getPowerState())
                .createdAt(vm.getCreatedAt())
                .vcpu(vm.getVcpu())
                .memoryMb(vm.getMemoryMb())
                .rootDiskGb(vm.getRootDiskGb())
                .ip(ip)
                .osImageName(osImage != null ? osImage.getName() : null)
                .osType(osType)
                .build();
    }
}
