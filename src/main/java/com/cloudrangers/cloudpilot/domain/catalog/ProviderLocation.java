package com.cloudrangers.cloudpilot.domain.catalog;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "provider_location")
public class ProviderLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 예: ap-northeast-2a, DC1/ClusterA */
    @Column(nullable = false)
    private String name;

    /** 상위 프로바이더(계정/커넥션) ID – 응답의 providerId 로 매핑 */
    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    /** AWS, VSPHERE 등 */
    @Column(name = "provider_type", nullable = false)
    private String providerType;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
