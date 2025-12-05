// src/main/java/com/cloudrangers/cloudpilot/dto/user/MyPageVmResponse.java
package com.cloudrangers.cloudpilot.dto.user;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MyPageVmResponse {

    private Long id;
    private String name;
    private String type;       // "퍼블릭" / "프라이빗" 등
    private String status;     // "running", "stopped", "pending"
//    private String cpu;
//    private String memory;
//    private String storage;
    private String os;
    private String ipAddress;
    private int cpu;
    private int memory;
    private int storage;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;

    private String ownerName;      // VM 소유자 이름
    private Long ownerId;
    private Long teamId;
    private String teamName;

    private List<String> packages;

    private List<AssignedMemberResponse> assignedMembers;
}
