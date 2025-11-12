package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.catalog.SecurityProfile;
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
public class SecurityProfileResponse {
    private String id;          // sg-web, nsx-db
    private String name;        // sg-web, nsx-db-seg
    private Long providerId;
    private Long zoneId;
    private List<String> rules; // ["80/tcp","443/tcp"]

    public static SecurityProfileResponse fromEntity(SecurityProfile e) {
        return SecurityProfileResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .providerId(e.getProviderId())
                .zoneId(e.getZoneId())
                .rules(splitCsv(e.getRulesCsv()))
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
