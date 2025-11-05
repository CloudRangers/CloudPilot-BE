package com.cloudrangers.cloudpilot.domain.provision;

import com.cloudrangers.cloudpilot.enums.ProviderType;
import com.cloudrangers.cloudpilot.enums.ProvisionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "provision_jobs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProvisionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String jobId;

    @Column(nullable = false)
    private Long catalogId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long teamId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderType providerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProvisionStatus status = ProvisionStatus.QUEUED;

    @Column(columnDefinition = "TEXT")
    private String terraformConfig;

    @Column(columnDefinition = "TEXT")
    private String provisionResult;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column
    private String vmResourceId;

    @Column
    private Integer retryCount;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 비즈니스 메서드
    public void updateStatus(ProvisionStatus newStatus) {
        this.status = newStatus;

        if (newStatus == ProvisionStatus.RUNNING && this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        } else if (newStatus == ProvisionStatus.SUCCEEDED ||
                newStatus == ProvisionStatus.FAILED) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public void updateStartedAt(LocalDateTime startedAt) {
        if (startedAt != null) {
            this.startedAt = startedAt;
        }
    }

    public void markAsSucceeded(String vmResourceId, String result) {
        this.status = ProvisionStatus.SUCCEEDED;
        this.vmResourceId = vmResourceId;
        this.provisionResult = result;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = ProvisionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null) ? 1 : this.retryCount + 1;
    }

    public boolean canRetry() {
        return this.retryCount == null || this.retryCount < 3;
    }
}
