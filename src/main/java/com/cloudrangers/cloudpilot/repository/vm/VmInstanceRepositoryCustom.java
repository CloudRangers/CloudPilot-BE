package com.cloudrangers.cloudpilot.repository.vm;

import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import com.cloudrangers.cloudpilot.dto.request.VmSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VmInstanceRepositoryCustom {
    Page<VmInstance> search(VmSearchCondition c, Pageable pageable, String sort);
}
