package com.cloudrangers.cloudpilot.domain.catalog;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "package_catalog")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PackageCatalog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;       // ex) jdk, nginx
    private String version;    // ex) 17.0.11+9, 1.25.5
    private String osFamily;   // "ubuntu,centos"
    private String arch;       // "x86_64,arm64"
    private String repo;       // ex) internal-artifact, internal-apt
}
