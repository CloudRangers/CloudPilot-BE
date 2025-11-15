package com.cloudrangers.cloudpilot.domain.provision;

import com.cloudrangers.cloudpilot.enums.VmProvisionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "vm_provision_job",
        indexes = {
                @Index(name = "idx_provision_job_status", columnList = "status, created_at"),
                @Index(name = "idx_provision_job_team", columnList = "team_id, created_at"),
                @Index(name = "idx_provision_job_zone", columnList = "zone_id")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class VmProvisionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "job_uuid", nullable = false, unique = true, length = 36)
    private String jobUuid;

    @Column(name = "catalog_id", nullable = false)
    private Long catalogId;

    // 외래키는 우선 ID로만 보유 (team, user, zone)
    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "zone_id", nullable = false)
    private Short zoneId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VmProvisionStatus status = VmProvisionStatus.queued;

    @Column(name = "purpose", length = 500)
    private String purpose;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 3;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_guide", columnDefinition = "TEXT")
    private String retryGuide;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (jobUuid == null) jobUuid = java.util.UUID.randomUUID().toString();
        if (updatedBy == null) updatedBy = createdBy;
    }
}
