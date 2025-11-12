package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.catalog.TerraformModule;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TerraformModuleResponse {
    private String id;
    private String moduleName;
    private String version;
    private String providerType;
    private List<String> variables;

    public static TerraformModuleResponse fromEntity(TerraformModule m) {
        return TerraformModuleResponse.builder()
                .id(m.getId())
                .moduleName(m.getModuleName())
                .version(m.getVersion())
                .providerType(m.getProviderType())
                .variables(m.getVariables())
                .build();
    }
}
