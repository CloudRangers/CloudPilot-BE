package com.cloudrangers.cloudpilot.service.catalog;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.request.ProviderCatalogSearchCondition;
import com.cloudrangers.cloudpilot.dto.response.ProviderCatalogItem;
import com.cloudrangers.cloudpilot.domain.catalog.ProviderLocation;
import com.cloudrangers.cloudpilot.repository.catalog.ProviderLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogProviderService {

    private final ProviderLocationRepository providerLocationRepository;

    public ApiResponse<PageResponse<ProviderCatalogItem>> search(
            int page, int size, String sort,
            ProviderCatalogSearchCondition c
    ) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        Specification<ProviderLocation> spec = buildSpec(c);

        Page<ProviderLocation> pageData = providerLocationRepository.findAll(spec, pageable);
        List<ProviderCatalogItem> items = pageData.map(ProviderCatalogItem::fromEntity).toList();

        return ApiResponse.ok(PageResponse.of(
                items,
                pageData.getNumber(),
                pageData.getSize(),
                pageData.getTotalElements()
        ));
    }

    private Sort buildSort(String sort) {
        // 기본값: "createdAt,desc"
        String s = (sort == null || sort.isBlank()) ? "createdAt,desc" : sort;
        String[] parts = s.split(",");
        String key = parts[0].trim();
        String dir = (parts.length > 1 ? parts[1].trim().toLowerCase() : "asc");

        // 화이트리스트 (보안/안전)
        if (!List.of("createdAt", "name", "providerType", "providerId", "id").contains(key)) {
            key = "createdAt";
        }
        return "desc".equals(dir) ? Sort.by(key).descending() : Sort.by(key).ascending();
    }

    private Specification<ProviderLocation> buildSpec(ProviderCatalogSearchCondition c) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (c == null) return cb.and(ps.toArray(new Predicate[0]));

            if (c.getProviderType() != null && !c.getProviderType().isBlank()) {
                ps.add(cb.equal(root.get("providerType"), c.getProviderType()));
            }
            if (c.getProviderId() != null) {
                ps.add(cb.equal(root.get("providerId"), c.getProviderId()));
            }
            if (c.getNameContains() != null && !c.getNameContains().isBlank()) {
                ps.add(cb.like(cb.lower(root.get("name")), "%" + c.getNameContains().toLowerCase() + "%"));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
