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
@Table(name = "instance_type_catalog")
public class InstanceTypeCatalog {
    @Id
    private String id;      // ex) "t3.micro" or "custom-2c4g"
    private String name;    // 표시용 이름
    private Integer vcpu;
    private Integer memoryGiB;
    private Long providerId;
    private Long zoneId;
    private Boolean burstable; // true/false
}
