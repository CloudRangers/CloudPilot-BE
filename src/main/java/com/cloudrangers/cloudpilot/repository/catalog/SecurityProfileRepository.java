package com.cloudrangers.cloudpilot.repository.catalog;

import com.cloudrangers.cloudpilot.domain.catalog.SecurityProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SecurityProfileRepository
        extends JpaRepository<SecurityProfile, String>, JpaSpecificationExecutor<SecurityProfile> {
}
