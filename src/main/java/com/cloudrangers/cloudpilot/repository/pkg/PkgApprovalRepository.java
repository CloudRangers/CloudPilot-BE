package com.cloudrangers.cloudpilot.repository.pkg;

import com.cloudrangers.cloudpilot.domain.pkg.PkgApproval;
import com.cloudrangers.cloudpilot.domain.pkg.PkgRequest;
import com.cloudrangers.cloudpilot.enums.PkgApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PkgApprovalRepository extends JpaRepository<PkgApproval, Long> {

    List<PkgApproval> findByPkgRequest(PkgRequest request);

    Optional<PkgApproval> findFirstByPkgRequestAndStep(PkgRequest request, PkgApprovalStep step);
}
