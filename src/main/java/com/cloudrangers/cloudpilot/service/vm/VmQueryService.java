package com.cloudrangers.cloudpilot.service.vm;

import com.cloudrangers.cloudpilot.domain.catalog.OsImage;
import com.cloudrangers.cloudpilot.dto.request.VmSearchCondition;
import com.cloudrangers.cloudpilot.dto.response.VmStatusResponse;
import com.cloudrangers.cloudpilot.dto.response.VmDetailResponse;
import com.cloudrangers.cloudpilot.repository.catalog.OsImageRepository; // Added this import
import com.cloudrangers.cloudpilot.repository.vm.VmInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VmQueryService {

    private final VmInstanceRepository vmInstanceRepository;
    private final OsImageRepository osImageRepository; // Added this

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

        List<Long> osImageIds = pageResult.getContent().stream()
                .map(vm -> vm.getOsImageId())
                .filter(osImageId -> osImageId != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, OsImage> osImageMap = osImageRepository.findAllById(osImageIds).stream()
                .collect(Collectors.toMap(OsImage::getId, osImage -> osImage));

        List<VmStatusResponse> items = pageResult.getContent().stream()
                .map(vm -> {
                    OsImage osImage = vm.getOsImageId() != null ? osImageMap.get(vm.getOsImageId()) : null;
                    return VmStatusResponse.fromEntity(vm, osImage);
                })
                .collect(Collectors.toList());

        return PageResponse.of(items, pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements());
    }

    // 호환용 (컨트롤러 이전 버전 호출 시)
    public PageResponse<VmStatusResponse> getVms(
            int page, int size, String providerType, Long zoneId,
            String status, String powerState, String name,
            Long ownerUserId, Long teamId
    ) {
        return getVms(page, size, providerType, zoneId, status, powerState, name, ownerUserId, teamId,
                null, null, Collections.emptyMap(), null);
    }

    public VmDetailResponse getVmDetail(Long vmId) {
        var vm = vmInstanceRepository.findById(vmId)
                .orElseThrow(() -> new IllegalArgumentException("VM not found: " + vmId));
        // Also fetch osImage for detail if needed, or join in repository
        return VmDetailResponse.fromEntity(vm);
    }

    @Transactional
    public void requestDelete(Long vmId) {
        var vm = vmInstanceRepository.findById(vmId)
                .orElseThrow(() -> new IllegalArgumentException("VM not found: " + vmId));
        vm.setLifecycle("deleting");
    }

    /**
     * ⭐ VM 이름 중복 체크
     * - lifecycle = "running" 인 VM을 기준으로 중복 여부 확인
     * - teamId가 있으면 해당 팀 내에서만 체크, 없으면 전체에서 체크
     */
    public boolean isVmNameDuplicate(String name, Long teamId) {
        if (name == null || name.isBlank()) {
            return false;
        }

        String lifecycle = "running";

        if (teamId != null) {
            return vmInstanceRepository.existsByNameAndTeamIdAndLifecycle(name, teamId, lifecycle);
        }

        return vmInstanceRepository.existsByNameAndLifecycle(name, lifecycle);
    }
}
