// src/main/java/com/cloudrangers/cloudpilot/service/catalog/ProviderQueryService.java
package com.cloudrangers.cloudpilot.service.catalog;

import com.cloudrangers.cloudpilot.dto.response.ProviderSummaryResponse;
import com.cloudrangers.cloudpilot.domain.catalog.Provider; // ✅ 엔티티 제네릭 지정
import com.cloudrangers.cloudpilot.repository.catalog.ProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import static com.cloudrangers.cloudpilot.repository.catalog.ProviderSpecs.*;

@Service
@RequiredArgsConstructor
public class ProviderQueryService {

    private final ProviderRepository providerRepository;

    /**
     * 메인 조회 (검색어 q, 타입 필터, 정렬)
     * sort 예: "name,asc" / "providerType,desc"
     */
    public PageResponse<ProviderSummaryResponse> getProviders(
            int page, int size, String q, String providerType, String sort
    ) {
        Sort sortObj = resolveSort(sort, "name,asc");
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Specification<Provider> spec = Specification.where(nameContains(q))
                .and(typeEquals(providerType));

        var pageResult = providerRepository.findAll(spec, pageable)
                .map(ProviderSummaryResponse::fromEntity);

        return PageResponse.of(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements()
        );
    }

    /**
     * ✅ 컨트롤러가 (page, size, providerType) 3개 인자로 호출하는 경우용 오버로드
     * - 검색어(q) 없음
     * - 정렬 기본값 "name,asc"
     */
    public PageResponse<ProviderSummaryResponse> getProviders(
            int page, int size, String providerType
    ) {
        // 올바른 인자 순서: (page, size, q=null, providerType, sort)
        return getProviders(page, size, null, providerType, "name,asc");
    }

    private Sort resolveSort(String sort, String def) {
        String s = (sort == null || sort.isBlank()) ? def : sort;
        String[] parts = s.split(",", 2);
        String prop = parts[0].trim();
        String dir  = (parts.length > 1 ? parts[1].trim() : "asc");
        return "desc".equalsIgnoreCase(dir) ? Sort.by(prop).descending() : Sort.by(prop).ascending();
    }
}
