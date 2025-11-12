package com.cloudrangers.cloudpilot.repository.catalog;

import com.cloudrangers.cloudpilot.domain.catalog.ProviderLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProviderLocationRepository
        extends JpaRepository<ProviderLocation, Long>, JpaSpecificationExecutor<ProviderLocation> {
}
