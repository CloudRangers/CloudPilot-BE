package com.cloudrangers.cloudpilot.service.catalog;

import com.cloudrangers.cloudpilot.common.exception.CatalogConflictException;
import com.cloudrangers.cloudpilot.common.exception.CatalogNotFoundException;
import com.cloudrangers.cloudpilot.domain.catalog.Datastore;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import com.cloudrangers.cloudpilot.dto.response.DatastoreResponse;
import com.cloudrangers.cloudpilot.repository.catalog.DatastoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import static com.cloudrangers.cloudpilot.repository.catalog.DatastoreSpecs.*;

@Service
@RequiredArgsConstructor
public class DatastoreQueryService {

    private final DatastoreRepository repo;

    public PageResponse<DatastoreResponse> getDatastores(
            int page, int size,
            Long providerId, Long zoneId,
            String q, String type,
            Integer minFreeGiB,
            Integer requestedCapacityGiB, // 선택: 충돌 체크용
            String sort
    ) {
        Sort s = resolveSort(sort, "name,asc");
        Pageable pageable = PageRequest.of(page, size, s);

        Specification<Datastore> spec = Specification.where(alwaysTrue())
                .and(providerEquals(providerId))
                .and(zoneEquals(zoneId))
                .and(nameContains(q))
                .and(typeEquals(type))
                .and(freeGte(minFreeGiB));

        var pageResult = repo.findAll(spec, pageable).map(DatastoreResponse::fromEntity);

        if (pageResult.isEmpty()) {
            throw new CatalogNotFoundException("DATASTORE_NOT_FOUND", "조건에 맞는 데이터스토어가 없습니다.");
        }

        // 선택: 요청 용량이 들어온 경우 간단한 충돌 판정
        if (requestedCapacityGiB != null) {
            boolean existEnough = pageResult.getContent().stream()
                    .anyMatch(d -> d.getFreeGiB() != null && d.getFreeGiB() >= requestedCapacityGiB);
            if (!existEnough) {
                throw new CatalogConflictException("CAPACITY_CONFLICT", "요청한 용량 요구사항과 데이터스토어 여유 공간이 충돌합니다.");
            }
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
