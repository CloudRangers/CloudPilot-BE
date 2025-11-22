package com.cloudrangers.cloudpilot.domain.pkg;

import com.cloudrangers.cloudpilot.domain.user.User;
import com.cloudrangers.cloudpilot.enums.PkgApprovalResult;
import com.cloudrangers.cloudpilot.enums.PkgApprovalStep;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "pkg_approval",
        indexes = {
                @Index(name = "idx_pkg_approval_request", columnList = "pkg_request_id"),
                @Index(name = "idx_pkg_approval_approver", columnList = "approver_user_id")
        })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PkgApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 요청에 대한 승인인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pkg_request_id", nullable = false)
    private PkgRequest pkgRequest;

    // L1 / FINAL
    @Enumerated(EnumType.STRING)
    @Column(name = "step", nullable = false, length = 10)
    private PkgApprovalStep step;

    // 승인자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_user_id", nullable = false)
    private User approver;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private PkgApprovalResult result;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    @PrePersist
    void onCreate() {
        if (decidedAt == null) decidedAt = Instant.now();
    }
}
