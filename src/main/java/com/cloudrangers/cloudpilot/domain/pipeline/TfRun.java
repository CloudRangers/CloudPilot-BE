package com.cloudrangers.cloudpilot.domain.pipeline;

import com.cloudrangers.cloudpilot.enums.TfRunAction;
import com.cloudrangers.cloudpilot.enums.TfRunStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "tf_run",
        indexes = {
                @Index(name = "idx_tf_run_pipeline", columnList = "pipeline_id"),
                @Index(name = "idx_tf_run_status", columnList = "status")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class TfRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // N:1 (Pipeline:TfRun)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pipeline_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tf_run_pipeline"))
    private Pipeline pipeline;

    // 모듈/변수셋은 우선 ID로만
    @Column(name = "module_version_id", nullable = false)
    private Long moduleVersionId;

    @Column(name = "varset_id", nullable = false)
    private Long varsetId;

    @Column(name = "workspace", length = 200)
    private String workspace;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private TfRunAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TfRunStatus status = TfRunStatus.running;

    @Column(name = "state_backend", length = 200)
    private String stateBackend;

    @Column(name = "state_uri", length = 500)
    private String stateUri;

    @Column(name = "plan_json_uri", length = 500)
    private String planJsonUri;

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
