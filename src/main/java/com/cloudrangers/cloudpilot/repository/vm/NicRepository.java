package com.cloudrangers.cloudpilot.repository.vm;

import com.cloudrangers.cloudpilot.domain.vm.Nic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NicRepository extends JpaRepository<Nic, Long> {

    // 특정 VM 인스턴스에 연결된 NIC 전체 조회
    List<Nic> findByVmInstanceId(Long vmInstanceId);

    // 혹시 특정 IP로 NIC 찾고 싶을 때
    Nic findByPrivateIp(String privateIp);
}
