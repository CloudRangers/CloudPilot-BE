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
@Table(name = "security_profile")
public class SecurityProfile {
    @Id
    private String id;          // 예: "sg-web", "nsx-db"
    private String name;        // 예: "sg-web", "nsx-db-seg"
    private Long providerId;
    private Long zoneId;

    // 규칙 목록은 CSV로 저장 (예: "80/tcp,443/tcp" 또는 "5432/tcp")
    private String rulesCsv;
}
