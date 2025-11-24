package com.cloudrangers.cloudpilot.domain.pkg;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ans_package_result",
        indexes = {
                @Index(name = "idx_ans_pkg_result_run", columnList = "ans_run_id"),
                @Index(name = "idx_ans_pkg_result_vm", columnList = "vm_id"),
                @Index(name = "idx_ans_pkg_result_pkg", columnList = "package_name")
        }
)
public class AnsPackageResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ans_run_id", nullable = false)
    private Long ansRunId;

    @Column(name = "vm_id", nullable = false)
    private Long vmId;

    @Column(name = "package_name", nullable = false, length = 255)
    private String packageName;

    @Column(name = "version", nullable = false, length = 255)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InstallStatus status;

    @CreationTimestamp
    @Column(name = "ts", nullable = false, updatable = false)
    private LocalDateTime ts;

    public enum InstallStatus {
        installed,
        failed
    }
}

