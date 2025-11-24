package com.cloudrangers.cloudpilot.domain.pkg;

import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ans_run")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnsRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // DDL: vm_instance_id bigint NOT NULL
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vm_instance_id", nullable = false)
    private VmInstance vmInstance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunStatus status; // running / succeeded / failed

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @PrePersist
    void onCreate() {
        if (startedAt == null) startedAt = LocalDateTime.now();
    }

    public enum RunStatus { running, succeeded, failed }
}
