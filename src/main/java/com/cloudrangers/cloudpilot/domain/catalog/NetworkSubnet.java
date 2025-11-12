package com.cloudrangers.cloudpilot.domain.catalog;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "network_subnet")
public class NetworkSubnet {
    @Id
    private String id;          // 예: "subnet-0ab" or "pg-back-01"
    private String name;        // 예: "frontend-subnet-a"
    private String cidr;        // 예: "10.0.10.0/24"
    private Long zoneId;        // FK-like
    private Long providerId;    // FK-like

    // 목적(purpose) 배열은 CSV로 보관 (예: "frontend,backend")
    private String purposeCsv;
}
