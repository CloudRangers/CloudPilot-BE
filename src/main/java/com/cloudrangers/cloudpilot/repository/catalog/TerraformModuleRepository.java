package com.cloudrangers.cloudpilot.repository.catalog;

import com.cloudrangers.cloudpilot.domain.catalog.TerraformModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TerraformModuleRepository
        extends JpaRepository<TerraformModule, String>, JpaSpecificationExecutor<TerraformModule> {
}
