package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.catalog.NetworkSubnet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkResponse {
    private String id;          // subnet-0ab, pg-back-01
    private String name;        // frontend-subnet-a, PG-Backend
    private String cidr;        // 10.0.10.0/24
    private Long zoneId;
    private Long providerId;
    private List<String> purpose; // ["frontend"] 등

    public static NetworkResponse fromEntity(NetworkSubnet e) {
        return NetworkResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .cidr(e.getCidr())
                .zoneId(e.getZoneId())
                .providerId(e.getProviderId())
                .purpose(splitCsv(e.getPurposeCsv()))
                .build();
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
