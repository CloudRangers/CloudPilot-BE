package com.cloudrangers.cloudpilot.domain.pipeline;

import com.cloudrangers.cloudpilot.domain.provision.VmProvisionJob;
import com.cloudrangers.cloudpilot.enums.PipelineStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "pipeline",
        indexes = {
                @Index(name = "idx_pipeline_job", columnList = "provision_job_id"),
                @Index(name = "idx_pipeline_status", columnList = "status")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Pipeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // N:1 (Job:Pipeline) — 하나의 Job에 여러 Pipeline을 둘 수 있게 설계(DDL 제약 없음)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provision_job_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pipeline_job"))
    private VmProvisionJob provisionJob;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PipelineStatus status = PipelineStatus.queued;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
