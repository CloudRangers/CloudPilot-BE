package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.catalog.PackageDef;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PackageDefResponse {
    private final Long id;
    private final String name;
    private final String description;
    private final String version;
    private final String arch;

    @Builder
    public PackageDefResponse(Long id, String name, String description, String version, String arch) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
        this.arch = arch;
    }

    public static PackageDefResponse fromEntity(PackageDef entity) {
        return PackageDefResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .version(entity.getVersion())
                .arch(entity.getArch())
                .build();
    }
}
