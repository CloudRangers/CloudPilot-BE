package com.cloudrangers.cloudpilot.domain.catalog;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "playbook")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Playbook {
    @Id
    private String id;            // ex) pb-java17
    private String name;          // ex) install-java17

    // CSV 로 저장해 간단히 필터링: "java,jdk"
    private String tags;          // ex) "java,jdk"
    private String osFamily;      // ex) "ubuntu,centos"
    private String arch;          // ex) "x86_64,arm64"
    private String requiredVars;  // ex) "java_version"
}
