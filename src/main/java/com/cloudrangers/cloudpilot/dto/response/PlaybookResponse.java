package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.catalog.Playbook;
import lombok.*;

import java.util.Arrays;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PlaybookResponse {
    private String id;
    private String name;
    private List<String> tags;
    private List<String> osFamily;
    private List<String> requiredVars;

    public static PlaybookResponse fromEntity(Playbook p) {
        return PlaybookResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .tags(splitCsv(p.getTags()))
                .osFamily(splitCsv(p.getOsFamily()))
                .requiredVars(splitCsv(p.getRequiredVars()))
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
