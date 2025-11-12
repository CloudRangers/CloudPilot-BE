package com.cloudrangers.cloudpilot.repository.catalog;

import com.cloudrangers.cloudpilot.domain.catalog.TerraformVarSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TerraformVarSetRepository
        extends JpaRepository<TerraformVarSet, String>, JpaSpecificationExecutor<TerraformVarSet> {
}
