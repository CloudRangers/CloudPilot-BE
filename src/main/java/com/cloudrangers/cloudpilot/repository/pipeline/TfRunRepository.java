package com.cloudrangers.cloudpilot.repository.pipeline;

import com.cloudrangers.cloudpilot.domain.pipeline.TfRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TfRunRepository extends JpaRepository<TfRun, Long> {
}