package com.cloudrangers.cloudpilot.repository.catalog;

import com.cloudrangers.cloudpilot.domain.catalog.NetworkSubnet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NetworkRepository
        extends JpaRepository<NetworkSubnet, String>, JpaSpecificationExecutor<NetworkSubnet> {
}
