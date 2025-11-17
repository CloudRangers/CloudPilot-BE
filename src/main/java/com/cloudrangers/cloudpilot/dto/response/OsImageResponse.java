// src/main/java/com/cloudrangers/cloudpilot/dto/response/OsImageResponse.java
package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.catalog.OsImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OsImageResponse {

    private String id;          // imageId
    private String name;
    private String osFamily;
    private String arch;
    private Long providerId;
    private Long zoneId;

    public static OsImageResponse fromEntity(OsImage e) {
        return OsImageResponse.builder()
                .id(e.getImageId())
                .name(e.getName())
                .osFamily(e.getOsFamily())
                .arch(e.getArch())
                .providerId(e.getProviderId())
                .zoneId(e.getZoneId())
                .build();
    }
}
