package com.cloudrangers.cloudpilot.repository.pkg;

import com.cloudrangers.cloudpilot.domain.pkg.PkgRequest;
import com.cloudrangers.cloudpilot.enums.PkgRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PkgRequestRepository extends JpaRepository<PkgRequest, Long> {

    List<PkgRequest> findByStatus(PkgRequestStatus status);

    List<PkgRequest> findByRequestedBy_Id(Long userId);
}
