package com.cloudrangers.cloudpilot.repository;

import com.cloudrangers.cloudpilot.domain.MetricTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetricTargetRepository extends JpaRepository<MetricTarget, Long> {

    /**
     * MetricTarget.active == true 인 Row 중
     * 특정 VM + 특정 exporterType 조건으로 1건 조회
     */
    Optional<MetricTarget> findByVmInstanceIdAndExporterTypeAndActiveTrue(
            Long vmInstanceId,
            String exporterType
    );
    List<MetricTarget> findByVmInstanceIdInAndActiveTrue(List<Long> vmInstanceIds);
}
