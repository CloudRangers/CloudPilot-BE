package com.cloudrangers.cloudpilot.repository.catalog;

import com.cloudrangers.cloudpilot.domain.catalog.PackageDef;
import com.cloudrangers.cloudpilot.domain.catalog.PackageVer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PackageVerRepository extends JpaRepository<PackageVer, Long> {

    List<PackageVer> findByPackageDef(PackageDef def);

    Optional<PackageVer> findByPackageDefNameAndVersion(String name, String version);
}
