package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.catalog.TerraformVarSet;
import com.cloudrangers.cloudpilot.domain.catalog.VarSetScope;
import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TerraformVarSetResponse {
    private String id;
    private String name;
    private VarSetScope scope;
    private String moduleId;              // null 가능
    private Map<String, String> variables;

    public static TerraformVarSetResponse fromEntity(TerraformVarSet v) {
        return TerraformVarSetResponse.builder()
                .id(v.getId())
                .name(v.getName())
                .scope(v.getScope())
                .moduleId(v.getModuleId())
                .variables(v.getVariables())
                .build();
    }
}
