package com.cloudrangers.cloudpilot.repository.pkg;

import com.cloudrangers.cloudpilot.domain.pkg.AnsRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnsRunRepository extends JpaRepository<AnsRun, Long> {
}
