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

    // ✅ 새로 추가된 필드들
    private final String lifecycle;   // 내부 상태(lifecycle) 별도 노출
    private final Long teamId;        // VM 소유 팀
    private final Long createdBy;     // 생성자
    private final Long ownerUserId;   // VM 소유 유저
    private final Instant updatedAt;  // 수정 시간

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
                            String osType,
                            String lifecycle,
                            Long teamId,
                            Long createdBy,
                            Long ownerUserId,
                            Instant updatedAt) {
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
        this.lifecycle = lifecycle;
        this.teamId = teamId;
        this.createdBy = createdBy;
        this.ownerUserId = ownerUserId;
        this.updatedAt = updatedAt;
    }

    /**
     * ✅ 기존 fromEntity(VmInstance, OsImage) 유지 + 필드 확장
     */
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
                .status(vm.getLifecycle())           // 기존 status 필드에 lifecycle 맵핑
                .powerState(vm.getPowerState())
                .lifecycle(vm.getLifecycle())        // 🔥 새 필드
                .teamId(vm.getTeamId())              // 🔥 새 필드
                .createdBy(vm.getCreatedBy())        // 🔥 새 필드
                .ownerUserId(vm.getOwnerUserId())    // 🔥 새 필드
                .createdAt(vm.getCreatedAt())
                .updatedAt(vm.getUpdatedAt())        // 🔥 새 필드
                .vcpu(vm.getVcpu())
                .memoryMb(vm.getMemoryMb())
                .rootDiskGb(vm.getRootDiskGb())
                .ip(ip)
                .osImageName(osImage != null ? osImage.getName() : null)
                .osType(osType)
                .build();
    }

    /**
     * ✅ 새로 추가: VmInstance → VmStatusResponse (OS 정보 없이)
     */
    public static VmStatusResponse from(VmInstance vm) {
        if (vm == null) return null;

        return VmStatusResponse.builder()
                .id(vm.getId())
                .name(vm.getName())
                .providerType(vm.getProviderType())
                .zoneId(vm.getZoneId())
                .status(vm.getLifecycle())
                .powerState(vm.getPowerState())
                .lifecycle(vm.getLifecycle())
                .teamId(vm.getTeamId())
                .createdBy(vm.getCreatedBy())
                .ownerUserId(vm.getOwnerUserId())
                .createdAt(vm.getCreatedAt())
                .updatedAt(vm.getUpdatedAt())
                .vcpu(vm.getVcpu())
                .memoryMb(vm.getMemoryMb())
                .rootDiskGb(vm.getRootDiskGb())
                .ip(vm.getIp())
                .osImageName(null)
                .osType(null)
                .build();
    }

    /**
     * ✅ 새로 추가: VmInstance + osImageName만 따로 내려주고 싶을 때
     */
    public static VmStatusResponse from(VmInstance vm, String osImageName) {
        if (vm == null) return null;

        return VmStatusResponse.builder()
                .id(vm.getId())
                .name(vm.getName())
                .providerType(vm.getProviderType())
                .zoneId(vm.getZoneId())
                .status(vm.getLifecycle())
                .powerState(vm.getPowerState())
                .lifecycle(vm.getLifecycle())
                .teamId(vm.getTeamId())
                .createdBy(vm.getCreatedBy())
                .ownerUserId(vm.getOwnerUserId())
                .createdAt(vm.getCreatedAt())
                .updatedAt(vm.getUpdatedAt())
                .vcpu(vm.getVcpu())
                .memoryMb(vm.getMemoryMb())
                .rootDiskGb(vm.getRootDiskGb())
                .ip(vm.getIp())
                .osImageName(osImageName)
                .osType(null)
                .build();
    }

    /**
     * 🔁 LEGACY 호환용
     * - 기존 코드에서 VmStatusResponse::fromEntity 를 사용하고 있어서
     *   시그니처만 맞춰서 내부적으로 from(...) 을 호출해줌.
     */
    public static VmStatusResponse fromEntity(VmInstance vm) {
        return from(vm);
    }
}
