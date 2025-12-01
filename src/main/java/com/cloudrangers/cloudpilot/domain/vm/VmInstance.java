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

    @Column(name = "provision_item_id")
    private Long provisionItemId;

    // 🔥 추가: 이 VM을 만든 Terraform 실행(tf_run)의 ID
    @Column(name = "tf_run_id")
    private Long tfRunId;

    // 🔥 추가: 해당 실행에서 사용한 terraform state 파일 경로(워커 로컬 경로)
    @Column(name = "state_uri", length = 1024)
    private String stateUri;

    private String name;           // VM 이름
    private String providerType;   // AWS / VSPHERE
    private Long zoneId;           // Zone 참조

    private String lifecycle;      // creating / running / deleting
    private String powerState;     // ON / OFF / SUSPENDED

    private Integer vcpu;
    private Integer memoryMb;
    private Integer rootDiskGb;

    private Long ownerUserId;
    private Long teamId;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    /**
     * CloudPilot에서 사용하는 VM 논리 이름
     * (vCenter VM name이랑 매핑해서 쓸 수 있는 부분)
     */
    @Column(name = "name", nullable = false)
    private String name;                // VM 이름

    /**
     * 🔹 클라우드/인프라 제공자 타입
     *   예) "VSPHERE", "AWS", "GCP" ...
     */
    @Column(name = "provider_type", nullable = false)
    private String providerType;        // 예: "AWS", "VSPHERE"

    /**
     * 🔹 실제 인프라(예: vCenter)에서 쓰는 VM ID
     *   - vCenter: "vm-324" 이런 값
     */
    @Column(name = "provider_instance_id", nullable = false)
    private String providerInstanceId;  // vSphere vm-XXX, EC2 i-XXXX 등

    @Column(name = "vcpu", nullable = false)
    private Integer vcpu;

    @Column(name = "memory_mb", nullable = false)
    private Integer memoryMb;

    @Column(name = "root_disk_gb", nullable = false)
    private Integer rootDiskGb;

    /**
     * 예) "POWERED_ON", "POWERED_OFF"
     */
    @Column(name = "power_state", nullable = false)
    private String powerState;

    /**
     * 예) "ACTIVE", "TERMINATED", "PENDING" 등
     */
    @Column(name = "lifecycle", nullable = false)
    private String lifecycle;

    /**
     * 확장용 JSON 컬럼 (태그/메타데이터)
     */
    @Column(name = "tags", columnDefinition = "json")
    private String tags;

    /**
     * Prometheus node-exporter 가 붙는 대표 IP
     */
    @Column(name = "ip")
    private String ip;

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
