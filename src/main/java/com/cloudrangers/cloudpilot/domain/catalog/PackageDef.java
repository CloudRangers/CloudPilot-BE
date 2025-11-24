package com.cloudrangers.cloudpilot.domain.catalog;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "package_def")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PackageDef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;
}
