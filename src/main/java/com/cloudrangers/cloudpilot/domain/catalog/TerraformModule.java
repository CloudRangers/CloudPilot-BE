package com.cloudrangers.cloudpilot.domain.catalog;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "terraform_module")
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class TerraformModule {

    @Id
    @Column(length = 100)
    private String id;              // 예: "mod-aws-ec2"

    @Column(nullable = false, length = 200)
    private String moduleName;      // 예: "cloudrangers/ec2-instance"

    @Column(nullable = false, length = 50)
    private String version;         // 예: "1.4.2"

    @Column(nullable = false, length = 50)
    private String providerType;    // 예: "AWS" | "VSPHERE" 등

    @ElementCollection
    @CollectionTable(name = "terraform_module_variables",
            joinColumns = @JoinColumn(name = "module_id"))
    @Column(name = "var_name", length = 100, nullable = false)
    @Builder.Default
    private List<String> variables = new ArrayList<>();
}
