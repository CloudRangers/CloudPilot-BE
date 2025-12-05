// src/main/java/com/cloudrangers/cloudpilot/repository/vm/VmInstanceRepository.java
package com.cloudrangers.cloudpilot.repository.vm;

import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VmInstanceRepository
        extends JpaRepository<VmInstance, Long>, VmInstanceRepositoryCustom {

    /**
     * vCenter에서 넘어온 VM 이름 리스트 기준으로
     * 우리 DB에 저장된 VM들을 한 번에 조회할 때 사용
     */
    List<VmInstance> findByNameIn(List<String> names);

    // ✅ 팀 필터 + 이름 리스트
    List<VmInstance> findByNameInAndTeamId(List<String> names, Long teamId);

    // ✅ 마이페이지용: 팀 기준 전체 VM 조회
    List<VmInstance> findByTeamId(Long teamId);

    List<VmInstance> findByTeamIdAndLifecycleIgnoreCase(Long teamId, String running);

    // ✅ VM 이름 + lifecycle 기준 존재 여부 (팀 상관 없음)
    boolean existsByNameAndLifecycle(String name, String lifecycle);

    // ✅ VM 이름 + 팀 + lifecycle 기준 존재 여부
    boolean existsByNameAndTeamIdAndLifecycle(String name, Long teamId, String lifecycle);
}
