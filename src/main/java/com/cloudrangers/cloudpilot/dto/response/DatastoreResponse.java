package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.catalog.Datastore;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatastoreResponse {
    private String id;
    private String name;
    private String type;     // vsan / nfs 등
    private Integer totalGiB;
    private Integer freeGiB;
    private Long zoneId;
    private Long providerId;

    public static DatastoreResponse fromEntity(Datastore e) {
        return DatastoreResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .type(e.getType())
                .totalGiB(e.getTotalGiB())
                .freeGiB(e.getFreeGiB())
                .zoneId(e.getZoneId())
                .providerId(e.getProviderId())
                .build();
    }
}
