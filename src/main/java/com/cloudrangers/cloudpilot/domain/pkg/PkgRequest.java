package com.cloudrangers.cloudpilot.domain.pkg;

import com.cloudrangers.cloudpilot.domain.user.User;
import com.cloudrangers.cloudpilot.enums.PkgRequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "pkg_request",
        indexes = {
                @Index(name = "idx_pkg_request_requested_by", columnList = "requested_by"),
                @Index(name = "idx_pkg_request_status", columnList = "status")
        })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PkgRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 요청한 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "package_name", nullable = false, length = 200)
    private String packageName;

    @Column(name = "package_ver", nullable = false, length = 20)
    private String packageVer;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PkgRequestStatus status;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @PrePersist
    void onCreate() {
        if (requestedAt == null) requestedAt = Instant.now();
        if (status == null) status = PkgRequestStatus.pending;
    }
}
