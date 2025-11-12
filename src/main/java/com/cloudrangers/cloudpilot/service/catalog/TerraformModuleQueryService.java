package com.cloudrangers.cloudpilot.service.catalog;

import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import com.cloudrangers.cloudpilot.domain.catalog.TerraformModule;
import com.cloudrangers.cloudpilot.dto.response.TerraformModuleResponse;
import com.cloudrangers.cloudpilot.repository.catalog.TerraformModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import static com.cloudrangers.cloudpilot.repository.catalog.TerraformSpecs.*;

@Service
@RequiredArgsConstructor
public class TerraformModuleQueryService {

    private final TerraformModuleRepository moduleRepository;

    public PageResponse<TerraformModuleResponse> getModules(
            int page, int size, String providerType, String q, String version, String sort
    ) {
        Sort sortObj = resolveSort(sort, "moduleName,asc");
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Specification<TerraformModule> spec = Specification
                .where(moduleNameContains(q))
                .and(moduleProviderTypeEquals(providerType))
                .and(moduleVersionEquals(version));

        Page<TerraformModuleResponse> pageResult = moduleRepository.findAll(spec, pageable)
                .map(TerraformModuleResponse::fromEntity);

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
