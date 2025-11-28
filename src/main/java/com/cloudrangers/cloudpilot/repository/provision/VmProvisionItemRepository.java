package com.cloudrangers.cloudpilot.repository.provision;

import com.cloudrangers.cloudpilot.domain.provision.VmProvisionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VmProvisionItemRepository extends JpaRepository<VmProvisionItem, Long> {

    List<VmProvisionItem> findByProvisionJobId(Long provisionJobId);

    List<VmProvisionItem> findByTfRunId(Long tfRunId);
}