package com.cloudrangers.cloudpilot.service.catalog;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.common.exception.CatalogNotFoundException;
import com.cloudrangers.cloudpilot.domain.catalog.NetworkSubnet;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import com.cloudrangers.cloudpilot.dto.response.NetworkResponse;
import com.cloudrangers.cloudpilot.repository.catalog.NetworkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import static com.cloudrangers.cloudpilot.repository.catalog.NetworkSpecs.*;

@Service
@RequiredArgsConstructor
public class NetworkQueryService {

    private final NetworkRepository networkRepository;

    public PageResponse<NetworkResponse> getNetworks(
            int page, int size, Long providerId, Long zoneId, String q, String sort
    ) {
        Sort sortObj = resolveSort(sort, "name,asc");
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Specification<NetworkSubnet> spec = Specification.where(alwaysTrue())
                .and(providerEquals(providerId))
                .and(zoneEquals(zoneId))
                .and(nameContains(q));

        var pageResult = networkRepository.findAll(spec, pageable)
                .map(NetworkResponse::fromEntity);

        if (pageResult.isEmpty()) {
            throw new CatalogNotFoundException("NETWORK_NOT_FOUND", "조건에 맞는 네트워크가 없습니다.");
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
