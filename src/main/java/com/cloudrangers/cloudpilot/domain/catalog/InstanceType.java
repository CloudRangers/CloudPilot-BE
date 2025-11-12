package com.cloudrangers.cloudpilot.domain.catalog;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "instance_type")
public class InstanceType {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ex) t3.micro / custom-2c4g */
    @Column(name = "code", unique = true)
    private String code;

    /** 표시용 이름 */
    @Column(name = "name")
    private String name;

    private Integer vcpu;         // 개수
    private Integer memoryGiB;    // GiB
    private Integer diskGb;       // 선택: 루트디스크(없어도 됨)

    /** AWS/GCP/vSphere 공급자 및 구역 */
    private Long providerId;
    private Long zoneId;

    /** ex) t-시리즈 같은 burstable 여부 */
    private Boolean burstable;
}
