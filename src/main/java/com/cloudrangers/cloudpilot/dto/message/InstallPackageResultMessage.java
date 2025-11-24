package com.cloudrangers.cloudpilot.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallPackageResultMessage {

    private String jobId;
    private Long vmId;
    private Long ansRunId;
    private boolean success;
    private String summary;

    private Map<String, String> packages;

    private List<TaskResult> tasks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaskResult {
        private String taskName;
        private boolean success;
        private String stdout;
        private String stderr;
    }
}
