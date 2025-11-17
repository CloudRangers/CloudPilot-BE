// src/main/java/com/cloudrangers/cloudpilot/dto/response/DeleteVmResponse.java
package com.cloudrangers.cloudpilot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteVmResponse {
    private String jobId;      // String (UUID 등)
    private Long vmId;
    private String status;     // "QUEUED" 등
    private Long requestedBy;  // 서비스에서 내려주는 필드가 있으니 DTO에도 추가
}
