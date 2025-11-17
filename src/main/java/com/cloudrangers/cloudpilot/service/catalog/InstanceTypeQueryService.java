package com.cloudrangers.cloudpilot.service.catalog;

import com.cloudrangers.cloudpilot.common.exception.CatalogNotFoundException;
import com.cloudrangers.cloudpilot.domain.catalog.InstanceTypeCatalog;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import com.cloudrangers.cloudpilot.dto.response.InstanceTypeResponse;
import com.cloudrangers.cloudpilot.repository.catalog.InstanceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import static com.cloudrangers.cloudpilot.repository.catalog.InstanceTypeSpecs.*;

@Service
@RequiredArgsConstructor
public class InstanceTypeQueryService {

    private final InstanceTypeRepository repo;

    public PageResponse<InstanceTypeResponse> getInstanceTypes(
            int page, int size,
            Long providerId, Long zoneId,
            String q,
            Integer vcpuMin, Integer vcpuMax,
            Integer memMinGiB, Integer memMaxGiB,
            Boolean burstable,
            String sort
    ) {
        Sort s = resolveSort(sort, "name,asc");
        Pageable pageable = PageRequest.of(page, size, s);

        Specification<InstanceTypeCatalog> spec = Specification.where(alwaysTrue())
                .and(providerEquals(providerId))
                .and(zoneEquals(zoneId))
                .and(nameContains(q))
                .and(vcpuGte(vcpuMin)).and(vcpuLte(vcpuMax))
                .and(memGte(memMinGiB)).and(memLte(memMaxGiB))
                .and(burstableEq(burstable));

        var pageResult = repo.findAll(spec, pageable).map(InstanceTypeResponse::fromEntity);

        if (pageResult.isEmpty()) {
            throw new CatalogNotFoundException("INSTANCE_TYPE_NOT_FOUND", "조건에 맞는 인스턴스 타입이 없습니다.");
        }

        return PageResponse.of(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements()
        );
    }

    private Sort resolveSort(String sort, String def) {
        String s = (sort == null || sort.isBlank()) ? def : sort;
        String[] parts = s.split(",", 2);
        String prop = parts[0].trim();
        String dir  = (parts.length > 1 ? parts[1].trim() : "asc");
        return "desc".equalsIgnoreCase(dir) ? Sort.by(prop).descending() : Sort.by(prop).ascending();
    }
}
