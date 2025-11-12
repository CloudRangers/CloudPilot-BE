package com.cloudrangers.cloudpilot.service.vm;

import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import com.cloudrangers.cloudpilot.dto.response.VmDetailResponse;
import com.cloudrangers.cloudpilot.repository.vm.VmInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class VmReadService {

    private final VmInstanceRepository vmInstanceRepository;

    public VmDetailResponse getVmDetail(Long vmId) {
        VmInstance vm = vmInstanceRepository.findById(vmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "VM not found: " + vmId));
        // 태그/메타는 현재 엔티티에 없다면 null/빈 맵으로 매핑
        return VmDetailResponse.fromEntity(vm);
    }
}
