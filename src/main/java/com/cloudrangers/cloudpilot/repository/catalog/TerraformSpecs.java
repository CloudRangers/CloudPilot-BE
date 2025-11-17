package com.cloudrangers.cloudpilot.repository.catalog;

import com.cloudrangers.cloudpilot.domain.catalog.TerraformModule;
import com.cloudrangers.cloudpilot.domain.catalog.TerraformVarSet;
import com.cloudrangers.cloudpilot.domain.catalog.VarSetScope;
import org.springframework.data.jpa.domain.Specification;

public final class TerraformSpecs {

    private TerraformSpecs() {}

    // ----- Module -----
    public static Specification<TerraformModule> moduleNameContains(String q) {
        return (root, cq, cb) -> (q == null || q.isBlank())
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("moduleName")), "%" + q.toLowerCase() + "%");
    }

    public static Specification<TerraformModule> moduleProviderTypeEquals(String providerType) {
        return (root, cq, cb) -> (providerType == null || providerType.isBlank())
                ? cb.conjunction()
                : cb.equal(root.get("providerType"), providerType);
    }

    // 간단한 버전: 완전 일치 (버전 제약식 파싱은 추후 확장)
    public static Specification<TerraformModule> moduleVersionEquals(String version) {
        return (root, cq, cb) -> (version == null || version.isBlank())
                ? cb.conjunction()
                : cb.equal(root.get("version"), version);
    }

    // ----- VarSet -----
    public static Specification<TerraformVarSet> varSetNameContains(String q) {
        return (root, cq, cb) -> (q == null || q.isBlank())
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%");
    }

    public static Specification<TerraformVarSet> varSetScopeEquals(VarSetScope scope) {
        return (root, cq, cb) -> (scope == null)
                ? cb.conjunction()
                : cb.equal(root.get("scope"), scope);
    }

    public static Specification<TerraformVarSet> varSetModuleIdEquals(String moduleId) {
        return (root, cq, cb) -> (moduleId == null || moduleId.isBlank())
                ? cb.conjunction()
                : cb.equal(root.get("moduleId"), moduleId);
    }
}
