package com.cloudrangers.cloudpilot.service.catalog;

import com.cloudrangers.cloudpilot.common.exception.CatalogNotFoundException;
import com.cloudrangers.cloudpilot.domain.catalog.OsImage;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import com.cloudrangers.cloudpilot.dto.response.OsImageResponse;
import com.cloudrangers.cloudpilot.repository.catalog.OsImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import static com.cloudrangers.cloudpilot.repository.catalog.OsImageSpecs.*;

@Service @RequiredArgsConstructor
public class OsImageQueryService {

    private final OsImageRepository osImageRepository;

    public PageResponse<OsImageResponse> getOsImages(
            int page, int size, Long providerId, Long zoneId, String q, String sort
    ) {
        Sort sortObj = resolveSort(sort, "name,asc");
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Specification<OsImage> spec = Specification.where(alwaysTrue())
                .and(providerEquals(providerId))
                .and(zoneEquals(zoneId))
                .and(nameOrFamilyContains(q));

        var pageResult = osImageRepository.findAll(spec, pageable)
                .map(OsImageResponse::fromEntity);

        // 결과 없음도 200으로 빈 리스트 내려도 되지만, 설계서가 404를 원하면 아래 사용
        if (pageResult.getTotalElements() == 0) {
            throw new CatalogNotFoundException("OS_IMAGE_NOT_FOUND", "조건에 맞는 OS 이미지가 없습니다.");
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
