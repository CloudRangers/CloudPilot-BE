package com.cloudrangers.cloudpilot.service.catalog;

import com.cloudrangers.cloudpilot.dto.response.PackageDefResponse;
import com.cloudrangers.cloudpilot.repository.catalog.PackageDefRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PackageDefService {

    private final PackageDefRepository packageDefRepository;

    public List<PackageDefResponse> findAll() {
        return packageDefRepository.findAll().stream()
                .map(PackageDefResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
