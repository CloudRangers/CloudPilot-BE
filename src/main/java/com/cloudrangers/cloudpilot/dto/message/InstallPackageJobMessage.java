package com.cloudrangers.cloudpilot.dto.message;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallPackageJobMessage {

    private String jobId;
    private Long vmId;
    private String hostname;
    private String ip;

    private Long ansRunId;
    private long requestedAt;

    private Long requestedByUserId;
    private Long requestedByEmpno;
    private String requestedByUsername;
    private String requestedByRole;
    private String requestedByTeam;

    private List<PackageItem> packages;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PackageItem {
        private String name;
        private String version;
    }
}
