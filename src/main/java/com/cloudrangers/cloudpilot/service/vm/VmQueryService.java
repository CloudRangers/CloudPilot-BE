package com.cloudrangers.cloudpilot.service.vm;

import com.cloudrangers.cloudpilot.dto.request.VmSearchCondition;
import com.cloudrangers.cloudpilot.dto.response.VmStatusResponse;
import com.cloudrangers.cloudpilot.dto.response.VmDetailResponse;   // ✅ 추가
import com.cloudrangers.cloudpilot.repository.vm.VmInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;        // ✅ 추가
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VmQueryService {

    private final VmInstanceRepository vmInstanceRepository;

    public PageResponse<VmStatusResponse> getVms(
            int page,
            int size,
            String providerType,
            Long zoneId,
            String status,
            String powerState,
            String name,
            Long ownerUserId,
            Long teamId,
            Instant createdFrom,
            Instant createdTo,
            Map<String, String> tagEquals,
            String sort
    ) {
        var pageable = PageRequest.of(page, size);

        var condition = VmSearchCondition.builder()
                .providerType(providerType)
                .zoneId(zoneId)
                .status(status)
                .powerState(powerState)
                .nameContains(name)
                .ownerUserId(ownerUserId)
                .teamId(teamId)
                .createdFrom(createdFrom)
                .createdTo(createdTo)
                .tagEquals(tagEquals)
                .build();

        var pageResult = vmInstanceRepository.search(condition, pageable, sort);
        List<VmStatusResponse> items = pageResult.map(VmStatusResponse::fromEntity).toList();

        return PageResponse.of(items, pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements());
    }

    // 호환용 (컨트롤러 이전 버전 호출 시)
    public PageResponse<VmStatusResponse> getVms(
            int page, int size, String providerType, Long zoneId,
            String status, String powerState, String name,
            Long ownerUserId, Long teamId
    ) {
        return getVms(page, size, providerType, zoneId, status, powerState, name, ownerUserId, teamId,
                null, null, null, null);
    }
    public VmDetailResponse getVmDetail(Long vmId) {
        var vm = vmInstanceRepository.findById(vmId)
                .orElseThrow(() -> new IllegalArgumentException("VM not found: " + vmId));
        return VmDetailResponse.fromEntity(vm);
    }
    @Transactional
    public void requestDelete(Long vmId) {
        var vm = vmInstanceRepository.findById(vmId)
                .orElseThrow(() -> new IllegalArgumentException("VM not found: " + vmId));
        vm.setLifecycle("deleting");
    }

}
