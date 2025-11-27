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

