package com.cloudrangers.cloudpilot.domain.vm;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "vm_instance")
public class VmInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * (레거시) 프로비저닝 아이템 ID
     */
    @Column(name = "provision_item_id")
    private Long provisionItemId;

    /**
     * 이 VM을 만든 Terraform 실행(tf_run)의 ID
     */
    @Column(name = "tf_run_id")
    private Long tfRunId;

    /**
     * Terraform state 파일 경로
     */
    @Column(name = "state_uri", length = 1024)
    private String stateUri;

    /**
     * OS 이미지 ID (os_image.id)
     */
    @Column(name = "os_image_id")
    private Long osImageId;

    /**
     * CloudPilot 논리 이름
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * 클라우드/인프라 제공자 타입
     */
    @Column(name = "provider_type", nullable = false)
    private String providerType;        // 예: "VSPHERE", "AWS"

    /**
     * 실제 인프라에서 쓰는 VM ID (예: vCenter "vm-324")
     */
    @Column(name = "provider_instance_id")
    private String providerInstanceId;

    /**
     * Zone 참조
     */
    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    /**
     * POWERED_ON / POWERED_OFF 등
     */
    @Column(name = "power_state", nullable = false)
    private String powerState;

    /**
     * ACTIVE / TERMINATED / PENDING 등
     */
    @Column(name = "lifecycle", nullable = false)
    private String lifecycle;

    @Column(name = "vcpu", nullable = false)
    private Integer vcpu;

    @Column(name = "memory_mb", nullable = false)
    private Integer memoryMb;

    @Column(name = "root_disk_gb", nullable = false)
    private Integer rootDiskGb;

    /**
     * VM 소유자(사용자) ID
     */
    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    /**
     * VM 소유 팀 ID
     */
    @Column(name = "team_id")
    private Long teamId;

    /**
     * 태그/메타데이터(JSON)
     */
    @Column(name = "tags", columnDefinition = "json")
    private String tags;

    /**
     * 대표 IP
     */
    @Column(name = "ip")
    private String ip;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
