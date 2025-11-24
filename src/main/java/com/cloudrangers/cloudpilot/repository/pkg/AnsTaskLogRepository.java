package com.cloudrangers.cloudpilot.repository.pkg;

import com.cloudrangers.cloudpilot.domain.pkg.AnsTaskLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnsTaskLogRepository extends JpaRepository<AnsTaskLog, Long> {
}
