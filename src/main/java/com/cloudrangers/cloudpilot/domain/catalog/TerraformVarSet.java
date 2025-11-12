package com.cloudrangers.cloudpilot.domain.catalog;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "terraform_varset")
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class TerraformVarSet {

    @Id
    @Column(length = 100)
    private String id;          // 예: "vs-global-defaults"

    @Column(nullable = false, length = 200)
    private String name;        // 예: "global-defaults"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VarSetScope scope;  // GLOBAL | TEAM | PROJECT

    // 필요하면 특정 모듈 전용 변수세트로 연결할 수도 있음(선택)
    @Column(length = 100)
    private String moduleId;    // null 가능

    @ElementCollection
    @CollectionTable(name = "terraform_varset_variables",
            joinColumns = @JoinColumn(name = "varset_id"))
    @MapKeyColumn(name = "key_name", length = 100)
    @Column(name = "value_text", length = 500)
    @Builder.Default
    private Map<String, String> variables = new HashMap<>();
}
