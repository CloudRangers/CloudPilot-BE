package com.cloudrangers.cloudpilot.domain.pkg;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ans_task_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnsTaskLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 Ansible 실행(AnsRun)에 속한 로그인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ans_run_id", nullable = false)
    private AnsRun ansRun;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;      // ok / changed / skipped / failed

    @Column(name = "stdout_snippet", columnDefinition = "TEXT")
    private String stdoutSnippet;

    @Column(name = "stderr_snippet", columnDefinition = "TEXT")
    private String stderrSnippet;

    @Column(name = "ts", nullable = false, updatable = false)
    private LocalDateTime ts;

    @PrePersist
    void onCreate() {
        if (ts == null) {
            ts = LocalDateTime.now();
        }
    }

    public enum TaskStatus {
        ok, changed, skipped, failed
    }
}
