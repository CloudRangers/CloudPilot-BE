package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * VM 상태 조회용 DTO
 *
 * 🔗 엔티티: com.cloudrangers.cloudpilot.domain.vm.VmInstance
 *  - 실제 DDL(vm_instance)에 존재하는 필드 기준으로만 매핑
 *  - 과거에 사용하던 providerType, zoneId, ip 등은 제거됨
 */
@Value
@Builder
public class VmStatusResponse {

    private Long id;
    private String name;

    // 🔄 CHANGED: providerType 제거됨 (엔티티/DDL에 없음)
    // private String providerType;

    private Integer vcpu;
    private Integer memoryMb;
    private Integer rootDiskGb;

    private String powerState;   // running / stopped / terminated
    private String lifecycle;    // creating / active / failed / deleting

    private Long teamId;
    private Long createdBy;

    private Instant createdAt;
    private Instant updatedAt;

    // 🆕 실제 DDL에 존재하는 컬럼
    private String providerInstanceId; // EC2 instance-id 또는 vSphere VM MOID 등
    private String tags;               // JSON 문자열 (필요 시 FE에서 파싱 가능)

    /**
     * ✅ 신규 표준 생성 메서드
     * 엔티티 -> DTO 변환
     */
    public static VmStatusResponse from(VmInstance vm) {
        return VmStatusResponse.builder()
                .id(vm.getId())
                .name(vm.getName())
                .vcpu(vm.getVcpu())
                .memoryMb(vm.getMemoryMb())
                .rootDiskGb(vm.getRootDiskGb())
                .powerState(vm.getPowerState())
                .lifecycle(vm.getLifecycle())
                .teamId(vm.getTeamId())
                .createdBy(vm.getCreatedBy())
                .createdAt(vm.getCreatedAt())
                .updatedAt(vm.getUpdatedAt())
                .providerInstanceId(vm.getProviderInstanceId())
                .tags(vm.getTags())
                .build();
    }

    /**
     * 🔁 LEGACY 호환용
     * - 기존 코드에서 VmStatusResponse::fromEntity 를 사용하고 있어서
     *   시그니처만 맞춰서 내부적으로 from(...) 을 호출해줌.
     * - 나중에 전체 코드 리팩토링할 때 fromEntity 호출부를 from(...) 으로 바꾸고
     *   이 메서드는 제거해도 됨.
     */
    public static VmStatusResponse fromEntity(VmInstance vm) {
        return from(vm);
    }
}
