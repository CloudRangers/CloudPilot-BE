package com.cloudrangers.cloudpilot.service.catalog;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.common.exception.CatalogNotFoundException;
import com.cloudrangers.cloudpilot.domain.catalog.Playbook;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import com.cloudrangers.cloudpilot.dto.response.PlaybookResponse;
import com.cloudrangers.cloudpilot.repository.catalog.PlaybookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaybookQueryService {

    private final PlaybookRepository playbookRepository;

    public PageResponse<PlaybookResponse> getPlaybooks(
            int page, int size,
            String q, String osFamily, String arch, String tag
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));

        Specification<Playbook> spec = Specification.where(alwaysTrue());
        if (q != null && !q.isBlank()) {
            spec = spec.and(nameOrIdContains(q));
        }
        if (osFamily != null && !osFamily.isBlank()) {
            spec = spec.and(csvContains("osFamily", osFamily));
        }
        if (arch != null && !arch.isBlank()) {
            spec = spec.and(csvContains("arch", arch));
        }
        if (tag != null && !tag.isBlank()) {
            spec = spec.and(csvContains("tags", tag));
        }

        Page<Playbook> pageResult = playbookRepository.findAll(spec, pageable);
        if (pageResult.getTotalElements() == 0) {
            throw new CatalogNotFoundException("PLAYBOOK_NOT_FOUND", "조건에 맞는 플레이북이 없습니다.");
        }

        List<PlaybookResponse> items = pageResult.map(PlaybookResponse::fromEntity).toList();
        return PageResponse.of(items, page, size, pageResult.getTotalElements());
    }

    private Specification<Playbook> alwaysTrue() {
        return (root, q, cb) -> cb.conjunction();
    }

    private Specification<Playbook> nameOrIdContains(String kw) {
        return (root, q, cb) -> {
            String like = "%" + kw.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("id")), like)
            );
        };
    }

    // CSV 컬럼 내부 포함 여부(tag, osFamily, arch, requiredVars 등)
    private Specification<Playbook> csvContains(String field, String token) {
        return (root, q, cb) -> {
            String like = "%" + token.toLowerCase() + "%";
            return cb.like(cb.lower(root.get(field)), like);
        };
    }
}
