// src/main/java/com/cloudrangers/cloudpilot/repository/catalog/ProviderRepository.java
package com.cloudrangers.cloudpilot.repository.catalog;

import com.cloudrangers.cloudpilot.domain.catalog.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProviderRepository
        extends JpaRepository<Provider, Long>, JpaSpecificationExecutor<Provider> {
}
