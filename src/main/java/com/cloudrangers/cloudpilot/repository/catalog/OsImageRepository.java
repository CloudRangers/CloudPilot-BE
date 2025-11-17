package com.cloudrangers.cloudpilot.repository.catalog;

import com.cloudrangers.cloudpilot.domain.catalog.OsImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OsImageRepository
        extends JpaRepository<OsImage, Long>, JpaSpecificationExecutor<OsImage> {
    // 이제 별도 메서드 없이도 spec 기반 조회 가능
}
