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

    private Instant createdAt;

    private String tags; // JSON 문자열로 저장될 수 있음
}
