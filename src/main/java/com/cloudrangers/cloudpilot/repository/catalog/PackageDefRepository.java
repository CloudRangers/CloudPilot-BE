package com.cloudrangers.cloudpilot.repository.catalog;

import com.cloudrangers.cloudpilot.domain.catalog.PackageDef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PackageDefRepository extends JpaRepository<PackageDef, Long> {

    Optional<PackageDef> findByName(String name);
}
