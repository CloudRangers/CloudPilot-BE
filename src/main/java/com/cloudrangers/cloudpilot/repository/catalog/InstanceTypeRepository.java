package com.cloudrangers.cloudpilot.repository.catalog;

import com.cloudrangers.cloudpilot.domain.catalog.InstanceTypeCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InstanceTypeRepository
        extends JpaRepository<InstanceTypeCatalog, String>, JpaSpecificationExecutor<InstanceTypeCatalog> {
}
