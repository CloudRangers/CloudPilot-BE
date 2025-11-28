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

    @Column(name = "os_image_id")
    private Long osImageId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    private String tags;

    @Column(name = "ip")
    private String ip;
}
