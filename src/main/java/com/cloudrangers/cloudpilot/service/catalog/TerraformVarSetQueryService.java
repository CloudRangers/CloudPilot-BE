package com.cloudrangers.cloudpilot.service.catalog;

import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import com.cloudrangers.cloudpilot.domain.catalog.TerraformVarSet;
import com.cloudrangers.cloudpilot.domain.catalog.VarSetScope;
import com.cloudrangers.cloudpilot.dto.response.TerraformVarSetResponse;
import com.cloudrangers.cloudpilot.repository.catalog.TerraformVarSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import static com.cloudrangers.cloudpilot.repository.catalog.TerraformSpecs.*;

@Service
@RequiredArgsConstructor
public class TerraformVarSetQueryService {

    private final TerraformVarSetRepository varSetRepository;

    public PageResponse<TerraformVarSetResponse> getVarSets(
            int page, int size, String scope, String moduleId, String q, String sort
    ) {
        Sort sortObj = resolveSort(sort, "name,asc");
        Pageable pageable = PageRequest.of(page, size, sortObj);

        VarSetScope scopeEnum = null;
        if (scope != null && !scope.isBlank()) {
            try {
                scopeEnum = VarSetScope.valueOf(scope.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 잘못된 scope는 그냥 필터 미적용 처리(또는 400 처리로 바꿔도 됨)
            }
        }

        Specification<TerraformVarSet> spec = Specification
                .where(varSetNameContains(q))
                .and(varSetScopeEquals(scopeEnum))
                .and(varSetModuleIdEquals(moduleId));

        Page<TerraformVarSetResponse> pageResult = varSetRepository.findAll(spec, pageable)
                .map(TerraformVarSetResponse::fromEntity);

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
