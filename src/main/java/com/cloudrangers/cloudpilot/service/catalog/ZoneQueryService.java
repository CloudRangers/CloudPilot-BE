// src/main/java/com/cloudrangers/cloudpilot/service/catalog/ZoneQueryService.java
package com.cloudrangers.cloudpilot.service.catalog;

import com.cloudrangers.cloudpilot.common.exception.CatalogNotFoundException;
import com.cloudrangers.cloudpilot.dto.response.ZoneResponse;
import com.cloudrangers.cloudpilot.domain.catalog.Zone;
import com.cloudrangers.cloudpilot.repository.catalog.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import static com.cloudrangers.cloudpilot.repository.catalog.ZoneSpecs.*;

@Service
@RequiredArgsConstructor
public class ZoneQueryService {

    private final ZoneRepository zoneRepository;

    public PageResponse<ZoneResponse> getZones(
            int page, int size, String q, String providerType, Long providerId, String sort // "name,asc"
    ) {
        Sort sortObj = resolveSort(sort, "name,asc");
        Pageable pageable = PageRequest.of(page, size, sortObj);

        // ✅ 제네릭 명시 + deprecated 회피
        Specification<Zone> spec = Specification.allOf(
                nameContains(q),
                typeEquals(providerType),
                providerIdEquals(providerId)
        );

        Page<Zone> pageResult = zoneRepository.findAll(spec, pageable);
        if (pageResult.isEmpty()) {
            throw new CatalogNotFoundException("ZONE_NOT_FOUND", "조건에 맞는 Zone이 없습니다.");
        }

        Page<ZoneResponse> mapped = pageResult.map(ZoneResponse::fromEntity);

        return PageResponse.of(
                mapped.getContent(),
                mapped.getNumber(),
                mapped.getSize(),
                mapped.getTotalElements()
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
