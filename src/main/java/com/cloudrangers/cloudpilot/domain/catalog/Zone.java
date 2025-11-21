package com.cloudrangers.cloudpilot.domain.catalog;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "zone")
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;   // zone.id

    /** 표시용 이름 (예: ap-northeast-2a, DC1/ClusterA) */
    @Column(name = "name", nullable = false)
    private String name;

    /** 존 타입 (예: VSPHERE, AWS 등) */
    @Column(name = "type")
    private String type;

    /** 리전 (예: ap-northeast-2, onprem-dc1 등) */
    @Column(name = "region")
    private String region;

    /** 존 관련 설정 JSON */
    @Column(name = "config_json")
    private String configJson;

    /** 생성 시각 */
    @Column(name = "created_at")
    private Instant createdAt;

    /** 선택: 외부 식별자(가용영역/클러스터 ID 등) */
    @Column(name = "external_id")
    private String externalId;

    /** 소속 프로바이더 (예: provider.id FK) */
    @Column(name = "provider_id")
    private Long providerId;

    /** 프로바이더 타입 (AWS / VSPHERE 등) */
    @Column(name = "provider_type")
    private String providerType;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
