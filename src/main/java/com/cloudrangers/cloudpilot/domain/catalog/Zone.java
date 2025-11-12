package com.cloudrangers.cloudpilot.domain.catalog;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "zone")
public class Zone {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 프로바이더 (예: AWS 계정 or vSphere 클러스터 식별자) */
    @Column(name = "provider_id")
    private Long providerId;

    /** 프로바이더 타입 (AWS / VSPHERE) */
    @Column(name = "provider_type")
    private String providerType;

    /** 표시용 이름 (예: ap-northeast-2a, DC1/ClusterA) */
    @Column(name = "name")
    private String name;

    /** 선택: 외부 식별자(가용영역/클러스터 ID 등) */
    @Column(name = "external_id")
    private String externalId;
}
