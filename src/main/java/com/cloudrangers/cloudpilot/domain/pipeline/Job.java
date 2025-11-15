package com.cloudrangers.cloudpilot.domain.pipeline;

import com.cloudrangers.cloudpilot.enums.JobStatus;
import com.cloudrangers.cloudpilot.enums.JobType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "job",
        indexes = {
                @Index(name = "uk_job_step", columnList = "pipeline_id, step_order", unique = true),
                @Index(name = "idx_job_pipeline", columnList = "pipeline_id, step_order"),
                @Index(name = "idx_job_status", columnList = "status")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // N:1 (Pipeline:Job)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pipeline_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_job_pipeline"))
    private Pipeline pipeline;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private JobType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status = JobStatus.queued;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "output_log", columnDefinition = "TEXT")
    private String outputLog;

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
