package com.cloudrangers.cloudpilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallPackagesRequest {

    @NotEmpty
    private List<Long> vmIds;

    @NotEmpty
    private List<PkgItem> packages;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PkgItem {
        @NotBlank
        private String name;
        @NotBlank
        private String version;
    }
}
