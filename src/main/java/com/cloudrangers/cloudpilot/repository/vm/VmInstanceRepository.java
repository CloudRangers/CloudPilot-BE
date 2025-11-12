package com.cloudrangers.cloudpilot.repository.vm;

import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VmInstanceRepository extends JpaRepository<VmInstance, Long>, VmInstanceRepositoryCustom {
}
