package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.enums.VmProvisionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * VM 프로비저닝 응답 (단일/다중 통합)
 *
 * 단일 생성(vmCount=1):
 *   - jobId: "101"
 *   - totalCount: 1
 *   - jobIds: [101]
 *   - batchId: null
 *
 * 다중 생성(vmCount=3):
 *   - jobId: null (또는 첫 번째 Job ID)
 *   - totalCount: 3
 *   - jobIds: [101, 102, 103]
 *   - batchId: "1a2b3c4d-..."
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvisionResponse {

    // ===== 공통 필드 (단일/다중 모두 사용) =====

    /**
     * 생성된 Job 개수 (vmCount와 동일)
     */
    private Integer totalCount;

    /**
     * 생성된 모든 Job ID 목록
     */
    private List<Long> jobIds;

    /**
     * 전체 상태
     */
    private VmProvisionStatus status;

    /**
     * 상태 메시지
     */
    private String message;

    /**
     * 생성 시각
     */
    private Instant createdAt;

    // ===== 단일 생성용 필드 (vmCount=1일 때만 사용) =====

    /**
     * Job ID (단일 생성 시)
     */
    private String jobId;

    /**
     * Catalog ID
     */
    private Long catalogId;

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 팀 ID
     */
    private Long teamId;

    /**
     * 에러 메시지 (실패 시)
     */
    private String errorMessage;

    /**
     * 재시도 횟수
     */
    private Integer retryCount;

    /**
     * 시작 시각
     */
    private Instant startedAt;

    /**
     * 완료 시각
     */
    private Instant completedAt;

    /**
     * 수정 시각
     */
    private Instant updatedAt;

    // ===== 다중 생성용 필드 (vmCount>1일 때만 사용) =====

    /**
     * 배치 ID (다중 생성 시)
     */
    private String batchId;

    /**
     * VM 리소스 ID (Worker에서 전달)
     */
    private String vmResourceId;

    // ===== 편의 메서드 =====

    /**
     * 단일 생성 여부
     */
    public boolean isSingleProvision() {
        return totalCount != null && totalCount == 1;
    }

    /**
     * 다중 생성 여부
     */
    public boolean isBatchProvision() {
        return totalCount != null && totalCount > 1;
    }

    /**
     * 첫 번째 Job ID 반환
     */
    public Long getFirstJobId() {
        return jobIds != null && !jobIds.isEmpty() ? jobIds.get(0) : null;
    }
}