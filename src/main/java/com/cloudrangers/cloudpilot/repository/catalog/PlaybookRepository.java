package com.cloudrangers.cloudpilot.repository.catalog;

import com.cloudrangers.cloudpilot.domain.catalog.Playbook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PlaybookRepository extends JpaRepository<Playbook, String>, JpaSpecificationExecutor<Playbook> {}
