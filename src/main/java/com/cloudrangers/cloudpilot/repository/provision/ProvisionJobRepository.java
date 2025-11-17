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

    // findById는 JpaRepository가 이미 제공하므로 별도 선언 불필요
    // JpaRepository<VmProvisionJob, Long>가 이미 Optional<VmProvisionJob> findById(Long id) 제공

    // userId -> createdBy (엔티티 필드명에 맞춤)
    List<VmProvisionJob> findByCreatedBy(Long createdBy);

    List<VmProvisionJob> findByTeamId(Long teamId);

    List<VmProvisionJob> findByStatus(VmProvisionStatus status);

    @Query("SELECT p FROM VmProvisionJob p WHERE p.status IN :statuses ORDER BY p.createdAt ASC")
    List<VmProvisionJob> findByStatusIn(@Param("statuses") List<VmProvisionStatus> statuses);

    @Query("SELECT p FROM VmProvisionJob p WHERE p.status = 'queued' ORDER BY p.createdAt ASC")
    List<VmProvisionJob> findQueuedJobs();

    @Query("SELECT p FROM VmProvisionJob p WHERE p.status = 'running' AND p.startedAt < :timeout")
    List<VmProvisionJob> findTimedOutJobs(@Param("timeout") Instant timeout);

    @Query("SELECT COUNT(p) FROM VmProvisionJob p WHERE p.teamId = :teamId AND p.status IN :statuses")
    long countByTeamIdAndStatusIn(@Param("teamId") Long teamId, @Param("statuses") List<VmProvisionStatus> statuses);

    @Query("SELECT p FROM VmProvisionJob p WHERE p.teamId = :teamId AND p.createdAt >= :startDate ORDER BY p.createdAt DESC")
    List<VmProvisionJob> findRecentJobsByTeam(@Param("teamId") Long teamId, @Param("startDate") Instant startDate);
}
