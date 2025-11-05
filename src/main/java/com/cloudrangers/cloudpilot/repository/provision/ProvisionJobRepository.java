package com.cloudrangers.cloudpilot.repository.provision;

import com.cloudrangers.cloudpilot.domain.provision.ProvisionJob;
import com.cloudrangers.cloudpilot.enums.ProvisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProvisionJobRepository extends JpaRepository<ProvisionJob, Long> {

    Optional<ProvisionJob> findByJobId(String jobId);

    List<ProvisionJob> findByUserId(Long userId);

    List<ProvisionJob> findByTeamId(Long teamId);

    List<ProvisionJob> findByStatus(ProvisionStatus status);

    @Query("SELECT p FROM ProvisionJob p WHERE p.status IN :statuses ORDER BY p.createdAt ASC")
    List<ProvisionJob> findByStatusIn(@Param("statuses") List<ProvisionStatus> statuses);

    @Query("SELECT p FROM ProvisionJob p WHERE p.status = 'QUEUED' ORDER BY p.createdAt ASC")
    List<ProvisionJob> findQueuedJobs();

    @Query("SELECT p FROM ProvisionJob p WHERE p.status = 'RUNNING' AND p.startedAt < :timeout")
    List<ProvisionJob> findTimedOutJobs(@Param("timeout") LocalDateTime timeout);

    @Query("SELECT COUNT(p) FROM ProvisionJob p WHERE p.teamId = :teamId AND p.status IN :statuses")
    long countByTeamIdAndStatusIn(@Param("teamId") Long teamId, @Param("statuses") List<ProvisionStatus> statuses);

    @Query("SELECT p FROM ProvisionJob p WHERE p.teamId = :teamId AND p.createdAt >= :startDate ORDER BY p.createdAt DESC")
    List<ProvisionJob> findRecentJobsByTeam(@Param("teamId") Long teamId, @Param("startDate") LocalDateTime startDate);
}
