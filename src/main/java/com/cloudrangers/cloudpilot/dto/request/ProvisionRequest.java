package com.cloudrangers.cloudpilot.dto.request;

import com.cloudrangers.cloudpilot.enums.ProviderType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * VM 프로비저닝 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvisionRequest {

    // ===== 필수 필드 =====

    /**
     * Zone ID (필수)
     */
    @NotNull(message = "zoneId는 필수입니다")
    private Integer zoneId;

    /**
     * CPU 코어 수 (필수)
     */
    @NotNull(message = "cpuCores는 필수입니다")
    @Min(value = 1, message = "cpuCores는 최소 1이어야 합니다")
    @Max(value = 128, message = "cpuCores는 최대 128까지 가능합니다")
    private Integer cpuCores;

    /**
     * 메모리 GB (필수)
     */
    @NotNull(message = "memoryGb는 필수입니다")
    @Min(value = 1, message = "memoryGb는 최소 1이어야 합니다")
    @Max(value = 1024, message = "memoryGb는 최대 1024까지 가능합니다")
    private Integer memoryGb;

    /**
     * 디스크 GB (필수)
     */
    @NotNull(message = "diskGb는 필수입니다")
    @Min(value = 10, message = "diskGb는 최소 10이어야 합니다")
    @Max(value = 10240, message = "diskGb는 최대 10240까지 가능합니다")
    private Integer diskGb;

    // ===== 선택 필드 =====

    /**
     * VM 이름 (선택, 생략 시 자동 생성)
     */
    private String vmName;

    /**
     * VM 개수 (선택, 기본값 1)
     * - 1: 단일 VM 생성
     * - N: N개의 VM을 개별 Job으로 생성
     */
    @Min(value = 1, message = "vmCount는 최소 1이어야 합니다")
    @Max(value = 100, message = "vmCount는 최대 100까지 가능합니다")
    private Integer vmCount;

    /**
     * Provider 타입 (선택, 기본값 VSPHERE)
     */
    private ProviderType providerType;

    /**
     * Catalog ID (선택)
     */
    private Long catalogId;

    /**
     * 목적/용도 (선택)
     */
    private String purpose;

    /**
     * 태그 목록 (선택)
     */
    private Map<String, String> tags;

    /**
     * 추가 설정 (선택)
     * - diskProvisioning: "thin" | "thick" | "thick_eager"
     * - ipAllocationMode: "DHCP" | "STATIC"
     * - templateName: "ubuntu-22.04-gold" 등
     */
    private Map<String, Object> additionalConfig;

    // ===== 편의 메서드 =====

    /**
     * vmCount 기본값 반환 (null이면 1)
     */
    public int getVmCountOrDefault() {
        return vmCount != null ? vmCount : 1;
    }

    /**
     * 단일 VM 생성 여부
     */
    public boolean isSingleProvision() {
        return getVmCountOrDefault() == 1;
    }

    /**
     * 다중 VM 생성 여부
     */
    public boolean isBatchProvision() {
        return getVmCountOrDefault() > 1;
    }
}