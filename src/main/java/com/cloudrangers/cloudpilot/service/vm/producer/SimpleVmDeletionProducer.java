package com.cloudrangers.cloudpilot.service.vm.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SimpleVmDeletionProducer implements VmDeletionProducer {
    @Override
    public void enqueue(String jobId, Long vmId, Long requestedBy) {
        // 실제 메시징 대신 로그로 대체
        log.info("[VM-DELETE-ENQUEUE] jobId={}, vmId={}, requestedBy={}", jobId, vmId, requestedBy);
    }
}
