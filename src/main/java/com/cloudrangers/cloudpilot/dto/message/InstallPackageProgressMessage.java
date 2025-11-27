package com.cloudrangers.cloudpilot.dto.message;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallPackageProgressMessage {
    private String jobId;
    private int progress;
    private String stage;
    private String message;
}
