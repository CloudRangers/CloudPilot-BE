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
@Table(name = "datastore")
public class Datastore {
    @Id
    private String id;       // ex) ds-vsan-01
    private String name;     // ex) vsanDatastore01
    private String type;     // ex) vsan, nfs
    private Integer totalGiB;
    private Integer freeGiB;
    private Long zoneId;
    private Long providerId;
}
