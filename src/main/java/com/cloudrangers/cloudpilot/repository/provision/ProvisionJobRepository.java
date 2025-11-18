package com.cloudrangers.cloudpilot.repository.provision;

import com.cloudrangers.cloudpilot.domain.provision.VmProvisionJob;
import com.cloudrangers.cloudpilot.enums.VmProvisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ProvisionJobRepository extends JpaRepository<VmProvisionJob, Long> {

    // ===== 기본 조회 (JpaRepository가 자동 제공) =====
    // Optional<VmProvisionJob> findById(Long id);
    // List<VmProvisionJob> findAllById(Iterable<Long> ids);

    /**
     * 사용자별 Job 조회
     */
    List<VmProvisionJob> findByCreatedBy(Long createdBy);

    /**
     * 팀별 Job 조회
     */
    List<VmProvisionJob> findByTeamId(Long teamId);

    /**
     * 상태별 Job 조회
     */
    List<VmProvisionJob> findByStatus(VmProvisionStatus status);

    /**
     * 여러 상태의 Job 조회 (정렬)
     */
    @Query("SELECT p FROM VmProvisionJob p WHERE p.status IN :statuses ORDER BY p.createdAt ASC")
    List<VmProvisionJob> findByStatusIn(@Param("statuses") List<VmProvisionStatus> statuses);

    /**
     * 대기 중인 Job 조회
     */
    @Query("SELECT p FROM VmProvisionJob p WHERE p.status = 'queued' ORDER BY p.createdAt ASC")
    List<VmProvisionJob> findQueuedJobs();

    /**
     * 타임아웃된 Job 조회 (실행 중이지만 오래된 것)
     */
    @Query("SELECT p FROM VmProvisionJob p WHERE p.status = 'running' AND p.startedAt < :timeout")
    List<VmProvisionJob> findTimedOutJobs(@Param("timeout") Instant timeout);

    /**
     * 팀별 상태별 카운트
     */
    @Query("SELECT COUNT(p) FROM VmProvisionJob p WHERE p.teamId = :teamId AND p.status IN :statuses")
    long countByTeamIdAndStatusIn(@Param("teamId") Long teamId, @Param("statuses") List<VmProvisionStatus> statuses);

    /**
     * 팀의 최근 Job 조회
     */
    @Query("SELECT p FROM VmProvisionJob p WHERE p.teamId = :teamId AND p.createdAt >= :startDate ORDER BY p.createdAt DESC")
    List<VmProvisionJob> findRecentJobsByTeam(@Param("teamId") Long teamId, @Param("startDate") Instant startDate);

    /**
     * Purpose에 특정 키워드 포함하는 Job 조회 (배치 검색용)
     * 예: purpose가 "[Batch: 1a2b3c4d"를 포함하는 Job들 검색
     */
    @Query("SELECT p FROM VmProvisionJob p WHERE p.teamId = :teamId AND p.purpose LIKE %:keyword% ORDER BY p.createdAt DESC")
    List<VmProvisionJob> findByTeamIdAndPurposeContaining(@Param("teamId") Long teamId, @Param("keyword") String keyword);
}