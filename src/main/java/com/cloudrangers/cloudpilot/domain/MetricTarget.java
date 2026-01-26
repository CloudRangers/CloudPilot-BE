package com.cloudrangers.cloudpilot.ops.domain.metric;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "metric_target")
public class MetricTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vm_instance_id", nullable = false)
    private Long vmInstanceId;

    @Column(name = "exporter_type", nullable = false)
    private String exporterType;

    @Column(name = "endpoint", nullable = false)
    private String endpoint;

    // 🔽🔽🔽 여기 추가 🔽🔽🔽
    /**
     * 🔄 CHANGED: DDL 기준 active 플래그 추가
     *  - MetricTargetRepository 에서
     *    findByVmInstanceIdAndExporterTypeAndActiveTrue(...)
     *    메서드를 사용하므로, 필드 이름이 정확히 active 여야 함
     */
    @Column(name = "active", nullable = false)
    private boolean active;
    // 🔼🔼🔼 여기까지 추가 🔼🔼🔼

    @Column(name = "scrape_url")
    private String scrapeUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
