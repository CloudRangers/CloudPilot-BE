package com.cloudrangers.cloudpilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PkgRequestCreateRequest {

    @NotBlank
    private String packageName;

    @NotBlank
    private String packageVer;

    private String description;
}
