package com.cloudrangers.cloudpilot.domain.catalog;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "os_image")
public class OsImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                 // 내부 PK (응답의 id 아님)

    @Column(name="provider_id")
    private Long providerId;

    @Column(name="provider_type")
    private String providerType;     // "AWS", "VSPHERE" 등

    @Column(name="zone_id")
    private Long zoneId;             // 소속 존

    @Column(name="name")
    private String name;             // 표시명 (e.g., Ubuntu 22.04 LTS)

    @Column(name="image_id")
    private String imageId;          // 실제 이미지 식별자 (예: AMI, Template ID)

    @Column(name="os_family")
    private String osFamily;         // "ubuntu", "centos", "windows" 등

    @Column(name="arch")
    private String arch;             // "x86_64", "arm64" 등
}
