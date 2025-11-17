package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.catalog.PackageCatalog;
import lombok.*;

import java.util.Arrays;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PackageResponse {
    private String name;
    private String version;
    private List<String> osFamily;
    private List<String> arch;
    private String repo;

    public static PackageResponse fromEntity(PackageCatalog e) {
        return PackageResponse.builder()
                .name(e.getName())
                .version(e.getVersion())
                .osFamily(splitCsv(e.getOsFamily()))
                .arch(splitCsv(e.getArch()))
                .repo(e.getRepo())
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
