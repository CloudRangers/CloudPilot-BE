package com.cloudrangers.cloudpilot.domain.catalog;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "package_ver")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PackageVer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private PackageDef packageDef;

    @Column(nullable = false)
    private String version;

    private String arch;
}
