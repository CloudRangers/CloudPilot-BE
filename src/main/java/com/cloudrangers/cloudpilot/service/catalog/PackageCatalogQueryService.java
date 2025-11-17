package com.cloudrangers.cloudpilot.service.catalog;

import com.cloudrangers.cloudpilot.common.exception.CatalogNotFoundException;
import com.cloudrangers.cloudpilot.domain.catalog.PackageCatalog;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import com.cloudrangers.cloudpilot.dto.response.PackageResponse;
import com.cloudrangers.cloudpilot.repository.catalog.PackageCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PackageCatalogQueryService {

    private final PackageCatalogRepository packageRepo;

    public PageResponse<PackageResponse> getPackages(
            int page, int size, String q, String osFamily, String arch, String repo
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));

        Specification<PackageCatalog> spec = Specification.where(alwaysTrue());
        if (q != null && !q.isBlank()) {
            spec = spec.and(nameContains(q));
        }
        if (osFamily != null && !osFamily.isBlank()) {
            spec = spec.and(csvContains("osFamily", osFamily));
        }
        if (arch != null && !arch.isBlank()) {
            spec = spec.and(csvContains("arch", arch));
        }
        if (repo != null && !repo.isBlank()) {
            spec = spec.and(equalString("repo", repo));
        }

        Page<PackageCatalog> pageResult = packageRepo.findAll(spec, pageable);
        if (pageResult.getTotalElements() == 0) {
            throw new CatalogNotFoundException("PACKAGE_NOT_FOUND", "조건에 맞는 패키지를 찾을 수 없습니다.");
        }

        var items = pageResult.map(PackageResponse::fromEntity).toList();
        return PageResponse.of(items, page, size, pageResult.getTotalElements());
    }

    private Specification<PackageCatalog> alwaysTrue() {
        return (r, q, cb) -> cb.conjunction();
    }

    private Specification<PackageCatalog> nameContains(String kw) {
        return (root, q, cb) -> cb.like(cb.lower(root.get("name")), "%" + kw.toLowerCase() + "%");
    }

    private Specification<PackageCatalog> equalString(String field, String value) {
        return (root, q, cb) -> cb.equal(cb.lower(root.get(field)), value.toLowerCase());
    }

    private Specification<PackageCatalog> csvContains(String field, String token) {
        return (root, q, cb) -> cb.like(cb.lower(root.get(field)), "%" + token.toLowerCase() + "%");
    }
}
