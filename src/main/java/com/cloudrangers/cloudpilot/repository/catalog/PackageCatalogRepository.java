package com.cloudrangers.cloudpilot.repository.catalog;

import com.cloudrangers.cloudpilot.domain.catalog.PackageCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PackageCatalogRepository extends JpaRepository<PackageCatalog, Long>, JpaSpecificationExecutor<PackageCatalog> {}
