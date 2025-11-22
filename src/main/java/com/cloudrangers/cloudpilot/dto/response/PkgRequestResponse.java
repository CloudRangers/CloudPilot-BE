package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.pkg.PkgRequest;
import com.cloudrangers.cloudpilot.enums.PkgRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class PkgRequestResponse {
    private Long id;
    private String packageName;
    private String packageVer;
    private String description;
    private PkgRequestStatus status;
    private Instant requestedAt;
    private Instant decidedAt;
    private Long requestedBy;

    public static PkgRequestResponse from(PkgRequest entity) {
        return PkgRequestResponse.builder()
                .id(entity.getId())
                .packageName(entity.getPackageName())
                .packageVer(entity.getPackageVer())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .requestedAt(entity.getRequestedAt())
                .decidedAt(entity.getDecidedAt())
                .requestedBy(entity.getRequestedBy().getId())
                .build();
    }
}
